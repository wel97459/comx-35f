package comx35

import spinal.core._
import spinal.core.sim._

object Config_ECP5_CLV8 {
  def spinal = SpinalConfig(
    targetDirectory = "./Colorlight_v8.0_ECP5/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = ASYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
object Config_ECP5_CLI5v6 {
  def spinal = SpinalConfig(
    targetDirectory = "./Colorlight_I5V6_ECP5/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = ASYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
