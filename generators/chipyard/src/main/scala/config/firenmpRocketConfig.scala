
package chipyard

import freechips.rocketchip.config.{Config}
import freechips.rocketchip.diplomacy.{AsynchronousCrossing}
import firenmp._

class firenmpRocketConfig extends Config(
    new freechips.rocketchip.subsystem.WithExtMemSbusBypass ++
    new firenmp.WithNMPAgent ++
    new freechips.rocketchip.subsystem.WithNBigCores(1) ++
    new chipyard.config.AbstractConfig
)
