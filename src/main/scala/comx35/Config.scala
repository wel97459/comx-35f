package comx35

import spinal.core._
import spinal.core.sim._

object Config_ECP5 {
  def spinal = SpinalConfig(
    targetDirectory = "./Colorlight_v8.0_ECP5/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = SYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
