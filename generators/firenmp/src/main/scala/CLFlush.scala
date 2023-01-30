package firenmp

import chisel3._ 
import chisel3.util._ // MuxCase
import testchipip.{TLHelper}
import freechips.rocketchip.tilelink.{TLMasterParameters}
import sifive.blocks.inclusivecache._ // L2 Control
import freechips.rocketchip.diplomacy._ // LazyModule
import freechips.rocketchip.config._


class CLFlushClient()(implicit p: Parameters) extends LazyModule{
  // create a tilelink client node
  val node = TLHelper.makeClientNode(TLMasterParameters.v1(
    name = "clflush-client",
    sourceId = IdRange(0, 1) // currently, it supports one transaction each time 
  ))
  lazy val module = new LazyModuleImp(this) {
    val (tl, edge) = node.out(0)
    val io = IO(new Bundle{
      // input is the flush address
      val flush_addr = Flipped(Decoupled(UInt(64.W)))
      // output is a flush done signal
      val flush_done = Output(Bool())
    })
    // cbus addr of the sifive l2 cache controller
    val cache_ctrl_addr = InclusiveCacheParameters.L2ControlAddress + 0x200 // flush64 offset
    val flush_addr = Reg(UInt(64.W))
    // state machine
    val s_init :: s_flush :: s_resp :: s_done :: Nil = Enum(4)
    val state = RegInit(s_init)
  
    val in_ready = RegInit(true.B)
    io.flush_addr.ready := in_ready
    // get the flush addr
    when(in_ready && io.flush_addr.valid){
      flush_addr := io.flush_addr.bits
      in_ready := false.B 
      state := s_flush
    }
    // only use a and d channels
    tl.a.valid := state === s_flush
    tl.a.bits := edge.Put(
      fromSource = 0.U,
      toAddress = cache_ctrl_addr.U,
      lgSize = log2Ceil(8).U, // 64bit
      data = flush_addr
    )._2

    tl.d.ready := state === s_resp
    // requests sent, waiting for response
    when (edge.done(tl.a)) {
      state := s_resp
    }
    // get the response from l2 control
    when (tl.d.fire) {
      state := s_done
      in_ready := true.B // now can recieve another inst
    } 
    io.flush_done := state === s_done
  }
}
