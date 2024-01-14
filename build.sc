import mill._, scalalib._

val spinalVersion = "1.10.0"

object mylib extends SbtModule {
  def scalaVersion = "2.12.18"
  override def millSourcePath = os.pwd
  def ivyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion",
    ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion",
    ivy"com.opencsv::opencsv:4.1"
  )
  def scalacPluginIvyDeps = Agg(ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion")
}