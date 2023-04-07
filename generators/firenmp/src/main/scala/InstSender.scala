
package firenmp

import chisel3._ 
import chisel3.util._ // MuxCase
import testchipip.{TLHelper}
import freechips.rocketchip.tilelink.{TLMasterParameters}
import sifive.blocks.inclusivecache._ // L2 Control
import freechips.rocketchip.diplomacy._ // LazyModule
import freechips.rocketchip.config._


class NMPInstSender()(implicit p: Parameters) extends LazyModule{
  // create a tilelink client node
  val node = TLHelper.makeClientNode(TLMasterParameters.v1(
    name = "nmp-inst-sender",
    sourceId = IdRange(0, 1) // currently, it supports one transaction each time 
  ))
  lazy val module = new LazyModuleImp(this) {
    val (tl, edge) = node.out(0)
    val io = IO(new Bundle{
      // input is the nmp instruction
      val insts = Flipped(Decoupled(new FireNMPCmd))
      val send_done = Output(Bool())
    })
    // cbus addr of the sifive l2 cache controller
    val mc_addr =  0x1080000000L + 0x200 // TODO: dertermine mc addr automatically 
    val in_cmd = Reg(new FireNMPCmd)

    // state machine
    val s_init :: s_send :: s_resp :: s_done :: Nil = Enum(4)
    val state = RegInit(s_init)
  
    val in_ready = RegInit(true.B)
    io.insts.ready := in_ready
    // get the flush addr
    when(in_ready && io.insts.valid){
      in_cmd := io.insts.bits    
      in_ready := false.B 
      state := s_send
    }
    // only use a and d channels
    tl.a.valid := state === s_send
    tl.a.bits := edge.Put(
      fromSource = 0.U,
      toAddress = mc_addr.U,
      lgSize = log2Ceil(17).U, // TODO: What's the actual size ?
      data = in_cmd.asUInt
    )._2

    tl.d.ready := state === s_resp
    // requests sent, waiting for response
    when (edge.done(tl.a)) {
      state := s_resp
    }
    when (tl.d.fire) {
      state := s_done
      in_ready := true.B // now can recieve another inst
    } 
    io.send_done := state === s_done
  }
}