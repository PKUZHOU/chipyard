// package firenmp

// import chisel3._
// import chisel3.util._
// import freechips.rocketchip.config.Parameters
// import freechips.rocketchip.tile.RoCCCommand

// class RoCCInstDecoder(implicit p: Parameters) extends Module {
//   val io = IO(new Bundle {
//     val in = Flipped(Decoupled(new RoCCCommand))
//     // val out = Decoupled(new FireNMPCmd)
//     val busy = Output(Bool())
//   })
// }