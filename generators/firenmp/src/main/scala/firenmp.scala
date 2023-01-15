package firenmp

import chisel3._ // VecInit
import chisel3.util._ // MuxCase
import freechips.rocketchip.tile._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._ // LazyModule
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink.{TLMasterParameters}
import sifive.blocks.inclusivecache._
import freechips.rocketchip.subsystem.{BaseSubsystem}

import testchipip.{TLHelper}

case class NMPAgentConfig()
case object NMPAgentKey extends  Field[Option[NMPAgentConfig]](None)

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

class NMPAgent(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(
  opcodes = opcodes){
  val flush_client = LazyModule(new CLFlushClient()(p))
  tlNode := flush_client.node 

  override lazy val module = new LazyRoCCModuleImp(this){
    val busy = RegInit(false.B) 
    val flush_addr = Reg(UInt(64.W))  
    val can_resp = RegInit(false.B)
    val funct = io.cmd.bits.inst.funct  // ignore the func now
    io.cmd.ready := !busy
    io.busy := busy

    val addr_valid = RegInit(false.B)
    flush_client.module.io.flush_addr.valid := addr_valid // set valid signal
    flush_client.module.io.flush_addr.bits := flush_addr  // feed flush addr to the flush client  
    
    when(io.cmd.fire){  //Rocket-core sends an instruction
      busy := true.B
      flush_addr := io.cmd.bits.rs1 // decode the flush addr
      addr_valid := true.B
    }

    when(flush_client.module.io.flush_done){
      can_resp := true.B
    }
    io.resp.valid := can_resp
    // addr sent, now invalidate the addr
    when(flush_client.module.io.flush_addr.fire){
      addr_valid := false.B
    }
    // reset the transaction
    when(io.resp.fire){
      can_resp := false.B
      busy := false.B
    }  
  }
} 

class WithNMPAgent extends Config((site, here, up) => {
  // case NMPAgentKey => Some(NMPAgentConfig())
  case BuildRoCC => Seq(
    (p: Parameters) => {
      val nmp_agent = LazyModule.apply(new NMPAgent(OpcodeSet.custom0)(p))
      nmp_agent  
    }
  ) 
})