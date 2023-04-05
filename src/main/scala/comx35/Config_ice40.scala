package comx35

import spinal.core._
import spinal.core.sim._

object Config_ice40 {
  def spinal = SpinalConfig(
    targetDirectory = "./UPduino3/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = SYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
