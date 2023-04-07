package chipyard

import freechips.rocketchip.config.{Config}
import freechips.rocketchip.diplomacy.{AsynchronousCrossing}
import cmsketch._

class CMsketchRocketConfig extends Config(
    new cmsketch.WithCMsketchAccel ++
    new freechips.rocketchip.subsystem.WithNBigCores(1) ++
    new chipyard.config.AbstractConfig
)
