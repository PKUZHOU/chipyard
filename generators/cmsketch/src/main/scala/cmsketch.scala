
package  cmsketch

import chisel3._
import chisel3.util.{HasBlackBoxResource}
import chisel3.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket.{TLBConfig, HellaCacheReq}

case object CmsketchWidth extends Field[Int] 
case object CmsketchDeepth extends Field[Int] 

class CMsketch(w:Int , d:Int) extends Module {
  val io = IO(new Bundle {
    val datain = Input(UInt(10.W))
    val datard = Input(UInt(10.W))
    val rden = Input(Bool())
    val wren = Input(Bool())
    val dataout = Output(UInt(10.W))
    val dataoutvalid = Output(Bool())
  })


  def Hash(x: UInt, n: UInt, m: Int): UInt = {
    val k = Wire(UInt(32.W))
    // k := (x<<m.U)
    k := x^m.U
    k%n
  }

  def countgen(max: UInt, en: Bool): UInt = {
    val cnt = RegInit(0.U(32.W))
    when(en) {
      cnt := Mux(cnt === max-1.U, 0.U, cnt + 1.U)
    }
    cnt
  }

  val Hashin = Wire(Vec(d, UInt(log2Up(w).W)))
  val Hashrd = Wire(Vec(d, UInt(log2Up(w).W)))
  val counter =Wire(Vec(w * d, UInt(32.W)))
  val enable = Wire(Vec(w * d, Bool()))

  for (i <- 0 until d) {
    when(io.wren) {
      Hashin(i) := Hash(io.datain, w.U, i + 1)
    }.otherwise{
      Hashin(i) := w.U
    }
  }

  when(io.wren){
  for (i <- 0 until d) {
    for (j <- 0 until w) {
      enable(i * w + j) := j.U === Hashin(i)
    }
  }}.otherwise{
    for (i <- 0 until d) {
      for (j <- 0 until w) {
        enable(i * w + j) := false.B
      }
    }
  }

    for (i <- 0 until d) {
      for (j <- 0 until w) {
        counter(i*w+j) := countgen(10000000.U, enable(i*w+j))
      }
    }


  val dataoutpre = Wire(Vec(d,UInt(32.W)))

  when(io.rden) {
    for (i <- 0 until d) {
      Hashrd(i) := Hash(io.datard,w.U,i+1)
    }
  }.otherwise{
    for (i <- 0 until d) {
      Hashrd(i) := 0.U
    }
  }
  val findmin = Wire(Bool())
  findmin := io.rden & !RegNext(io.rden)
  when(findmin){
    for(i <- 0 until d){
      dataoutpre(i) := counter(i.U*w.U + Hashrd(i))
    }
  }.otherwise{
    for (i <- 0 until d) {
      dataoutpre(i) := 0.U
    }
  }

  val datamin = Reg(UInt(32.W))

  when(findmin){
    for(i<-0 until(d)){
      when (i.U===0.U){
        datamin := dataoutpre(0)
      }.otherwise{
        when(dataoutpre(i)<datamin){
          datamin := dataoutpre(i)
        }
      }
    }
  }.otherwise{
    datamin := 10000000.U
  }

  val temp = RegInit(0.U(10.W))
  val tempwren = RegInit(false.B)

  when(io.wren){
    temp := io.datain
    tempwren := true.B
  }.otherwise{
    when(io.dataoutvalid){
      temp := 0.U
      tempwren := false.B
    }
  }


  io.dataoutvalid := io.rden & !(datamin === 10000000.U)
  io.dataout := Mux(io.dataoutvalid,Mux(tempwren & (temp === io.datard),datamin + 1.U,datamin),0.U)
}

class CMsketchAccel(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(
  opcodes = opcodes){
  override lazy val module = new CMsketchAccelImp(this)
}


class CMsketchAccelImp(outer:CMsketchAccel)(implicit p: Parameters) extends LazyRoCCModuleImp(outer) {
  chisel3.dontTouch(io)

  val w = p(CmsketchWidth)
  val d = p(CmsketchDeepth)

  val rden = RegInit(false.B)
  val wren = RegInit(false.B)
  val datain = RegInit(0.U(10.W))
  val datard = RegInit(0.U(10.W))
  val rd    = RegInit(0.U(5.W))

  val busy = RegInit(false.B)
  val canResp = RegInit(false.B)
  io.cmd.ready := !busy
  io.busy := busy

  val cm = Module(new CMsketch(w,d))

  val wrrd = io.cmd.fire() &&  (io.cmd.bits.inst.funct===3.U)
  val nwrrd = io.cmd.fire() && (io.cmd.bits.inst.funct===2.U)
  val wrnrd = io.cmd.fire() && (io.cmd.bits.inst.funct===1.U)
  val nwrnrd = io.cmd.fire() && (io.cmd.bits.inst.funct===0.U)
  // when(wrrd){
  //   busy := true.B
  //   rden := true.B
  //   wren := true.B
  //   datain := io.cmd.bits.rs1
  //   datard := io.cmd.bits.rs2
  //   rd := io.cmd.bits.inst.rd
  // }.otherwise{
  //   when(nwrrd){
  //       busy := true.B
  //       rden := true.B
  //       wren := false.B
  //       datain := io.cmd.bits.rs1
  //       datard := io.cmd.bits.rs2
  //       rd := io.cmd.bits.inst.rd
  //   }.otherwise{
  //     when(wrnrd){
  //           busy := true.B
  //           rden := false.B
            // wren := true.B
            // datain := io.cmd.bits.rs1
  //           datard := io.cmd.bits.rs2
  //           rd := io.cmd.bits.inst.rd
  //     }.otherwise{
  //       when(nwrnrd){
  //           busy := false.B
  //           rden := false.B
  //           wren := false.B
  //           datain := io.cmd.bits.rs1
  //           datard := io.cmd.bits.rs2
  //           rd := io.cmd.bits.inst.rd
  //       }.otherwise{
            // wren := false.B
            // datain := 0.U
  //       }
  //     }
  //   }
  // }
  when(io.resp.fire()){
    busy := false.B
    rden := false.B
    datard := 0.U
    rd := 0.U
  }.otherwise{
    when(wrrd){
    busy := true.B
    rden := true.B
    datard := io.cmd.bits.rs2
    rd := io.cmd.bits.inst.rd
  }.otherwise{
    when(nwrrd){
        busy := true.B
        rden := true.B
        datard := io.cmd.bits.rs2
        rd := io.cmd.bits.inst.rd
    }.otherwise{
      when(wrnrd){
            busy := true.B
            rden := false.B
            datard := io.cmd.bits.rs2
            rd := io.cmd.bits.inst.rd
      }.otherwise{
        when(nwrnrd){
            busy := false.B
            rden := false.B
            datard := io.cmd.bits.rs2
            rd := io.cmd.bits.inst.rd
        }
      }
    }
  }
}
  when(io.resp.fire()){
    wren := false.B
    datain := 0.U
  }.otherwise{
    when(wrrd){
      wren := true.B
      datain := io.cmd.bits.rs1
  }.otherwise{
    when(nwrrd){
       wren := false.B
       datain := 0.U
    }.otherwise{
      when(wrnrd){
       wren := true.B
       datain := io.cmd.bits.rs1
      }.otherwise{
        wren := false.B
        datain := 0.U
      }
    }
  }
}

  cm.io.wren := wren
  cm.io.rden := rden
  cm.io.datain := datain
  cm.io.datard := datard
  
  val cmRes = RegInit(0.U(10.W)) 

  // when(cm.io.dataoutvalid){
  //   canResp := true.B
  //   cmRes := cm.io.dataout
  // }
  io.resp.valid := canResp
  io.resp.bits.data := cmRes
  io.resp.bits.rd := rd
  // when(io.resp.fire()){
  //           canResp := false.B
  //           busy := false.B
  //           wren := false.B
  //           rden := false.B
  //           datain := 0.U
  //           datard := 0.U
  //           cmRes := 0.U
  //           rd := 0.U
  //       } 

  when(io.resp.fire()){
            canResp := false.B
            cmRes := 0.U
        }.otherwise{
          when(cm.io.dataoutvalid){
            canResp := true.B
            cmRes := cm.io.dataout
          }
      }
}

class WithCMsketchAccel extends Config ((site, here, up) => {
    case CmsketchWidth => 100
    case CmsketchDeepth => 5
    case BuildRoCC => up(BuildRoCC) ++ Seq(
    (p: Parameters) => {
      val cmsketch = LazyModule.apply(new CMsketchAccel(OpcodeSet.custom1)(p))
      cmsketch
    }
  )
})