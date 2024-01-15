ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / organization := "org.example"

val spinalVersion = "1.10.0"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)


val CSVlib = "com.opencsv" % "opencsv" % "4.1"

lazy val mylib = (project in file("."))
  .settings(
    name := "SpinalTemplateSbt",
    Compile / scalaSource := baseDirectory.value / "src" / "main" / "scala",
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin, CSVlib)
  )

fork := true
