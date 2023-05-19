package firenmp

import chisel3._ 
import chisel3.util._ // MuxCase
// import testchipip.{TLHelper}
import freechips.rocketchip.tilelink._
// import sifive.blocks.inclusivecache._ // L2 Control
import freechips.rocketchip.diplomacy._ // LazyModule
import freechips.rocketchip.config._

class NMPInstGet()(implicit p: Parameters) extends LazyModule {
    val node = TLClientNode(Seq(TLMasterPortParameters.v1(Seq(TLMasterParameters.v1(
    name = "nmp-inst-get",
    sourceId = IdRange(0, 1) // currently, it supports one transaction each time 
  )))))

  lazy val module = new NMPInstGetImp(this)
}

class NMPInstGetImp(outer: NMPInstGet)(implicit p: Parameters) extends LazyModuleImp(outer: NMPInstGet){
    val (tl, edge) = outer.node.out(0)
    val io = IO(new Bundle{
      val insts = Flipped(Decoupled(new FireNMPCmd))
      val get_done = Output(Bool())
    })

    val mc_addr = 0x1080100030L
    val in_cmd  = Reg(new(FireNMPCmd))

    val s_init :: s_send :: s_resp :: s_done :: Nil = Enum(4)

    val state = RegInit(s_init)
    val in_ready = RegInit(true.B)
    io.insts.ready := in_ready
    when(in_ready && io.insts.valid){
      in_cmd := io.insts.bits    
      in_ready := false.B 
      state := s_send
    }
    tl.a.valid := (state === s_send) 
    tl.a.bits := edge.Get(
      fromSource = 0.U,
      toAddress = mc_addr.U,
      lgSize = log2Ceil(8).U, // TODO: What's the actual size ?
    )._2
    tl.d.ready := state === s_resp
    when (edge.done(tl.a)) {
      state := s_resp
    }
    when (tl.d.fire) {
      state := s_done
      in_ready := true.B // now can recieve another inst
    } 
    io.get_done := state === s_done

    when (io.get_done){
      state := s_init
    }
}
