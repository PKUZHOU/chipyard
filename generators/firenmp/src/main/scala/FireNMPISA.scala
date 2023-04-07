package firenmp

import chisel3._

object FireNMPISA {
    //=====================
    //    funct values
    //=====================
    val CLFLUSH_CMD = 0.U
    val RD_CMD = 1.U
    val WR_CMD = 2.U   // Write data from PE buffer to bank  
    val ADD_CMD = 3.U  // Vec_out = Vec_A (in PE buffer) + Vec_B (in bank)
    val MAC_CMD = 4.U  // out = Vec_A (in PE buffer) * Vec_B (from bank)

    //====================
    //
    //====================        
}

class FireNMPCmd extends Bundle {
  val funct = Bits(7.W)
  val rs1 = Bits(64.W)
  val rs2 = Bits(64.W)
}