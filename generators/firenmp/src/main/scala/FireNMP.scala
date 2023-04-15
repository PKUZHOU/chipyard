package firenmp

import chisel3._ // VecInit
import chisel3.util._ // MuxCase
import freechips.rocketchip.tile._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._ // LazyModule
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem.{BaseSubsystem}
import freechips.rocketchip.tilelink._
// import freechips.rocketch

case class NMPAgentConfig()
case object NMPAgentKey extends  Field[Option[NMPAgentConfig]](None)

class NMPAgent(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(
  opcodes = opcodes){
  lazy val flush_client = LazyModule(new CLFlushClient()(p))
  lazy val cmd_sender = LazyModule(new NMPInstSender()(p))
  lazy val inst_get  = LazyModule(new NMPInstGet()(p))
  tlNode := flush_client.node
  tlNode := cmd_sender.node 
  tlNode := inst_get.node

  override lazy val module = new LazyRoCCModuleImp(this){
    val busy = RegInit(false.B) 
    val flush_addr = Reg(UInt(64.W))  
    val out_cmd = Reg(new FireNMPCmd)
    val can_resp = RegInit(false.B)
 
    io.cmd.ready := !busy
    io.busy := busy

    //TODO: PTW?
    val flush_addr_valid = RegInit(false.B)
    val nmp_cmd_valid = RegInit(false.B)
    val nmp_get_valid = RegInit(false.B)

    // flush_client.module.io.flush_addr.valid := flush_addr_valid // set valid signal
    flush_client.module.io.flush_addr.valid := flush_addr_valid
    flush_client.module.io.flush_addr.bits := flush_addr  // feed flush addr to the flush client  
    
    cmd_sender.module.io.insts.valid := nmp_cmd_valid
    cmd_sender.module.io.insts.bits := out_cmd    

    inst_get.module.io.insts.valid := nmp_get_valid
    inst_get.module.io.insts.bits  := out_cmd

    when(io.cmd.fire){  //Rocket-core sends an instruction
      busy := true.B
      switch(io.cmd.bits.inst.funct){
        is(FireNMPISA.CLFLUSH_CMD){
          flush_addr := io.cmd.bits.rs1 // decode the flush addr
          flush_addr_valid := true.B
        }
        is(FireNMPISA.ADD_CMD){
          out_cmd.funct := FireNMPISA.ADD_CMD
          out_cmd.rs1   := io.cmd.bits.rs1
          out_cmd.rs2   := io.cmd.bits.rs2
          nmp_cmd_valid := true.B
        }
        is(FireNMPISA.GET_CMD){
          nmp_get_valid := true.B 
          out_cmd.funct := FireNMPISA.GET_CMD
          out_cmd.rs1   := io.cmd.bits.rs1
          out_cmd.rs2   := io.cmd.bits.rs2
        }
      }      
    }
    io.resp.valid := can_resp
    when(flush_client.module.io.flush_done){
      can_resp := true.B
    }
    when(cmd_sender.module.io.send_done){
      can_resp := true.B
    }
    when(inst_get.module.io.get_done){
      can_resp := true.B
    }
    // addr sent, now invalidate the addr
    when(flush_client.module.io.flush_addr.fire){
      flush_addr_valid := false.B
    }
    when(cmd_sender.module.io.insts.fire){
      nmp_cmd_valid := false.B
    }
    when(inst_get.module.io.insts.fire){
      nmp_get_valid := false.B
    }
    // reset the transaction
    when(io.resp.fire){
      can_resp := false.B
      busy := false.B
      flush_addr := 0.U
      flush_addr_valid := false.B
      nmp_cmd_valid := false.B
      out_cmd.funct := 0.U
      out_cmd.rs1   := 0.U
      out_cmd.rs2   := 0.U
    }  
  }
} 

class WithNMPAgent extends Config((site, here, up) => {
  case BuildRoCC => Seq(
    (p: Parameters) => {
      val nmp_agent = LazyModule.apply(new NMPAgent(OpcodeSet.custom0)(p))
      nmp_agent  
    }
  ) 
})