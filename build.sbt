
val sharedSettings = Seq(
  name := "mzdb4s",
  organization := "com.github.mzdb",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.12",

  libraryDependencies ++= Seq(
    "tech.sparse" %%% "pine" % "0.1.6",
    "biz.enef" %%% "slogging" % "0.6.1",
    "com.lihaoyi" %%% "utest" % "0.6.7" % "test"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework")
)

lazy val mzdb4sCore = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("core"))
  .settings(sharedSettings)
  /*.jsSettings(/* ... */) // defined in sbt-scalajs-crossproject */
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.almworks.sqlite4java" % "sqlite4java" % "1.0.392",
      // OS dependant SQLite library dependency
      // See: https://stackoverflow.com/questions/55591045/sbt-dynamically-detect-building-platform
      sys.props("os.name").toLowerCase match {
        case win if win.contains("win") => "com.almworks.sqlite4java" % "sqlite4java-win32-x64" % "1.0.392"
        case linux if linux.contains("linux") => "com.almworks.sqlite4java" % "libsqlite4java-linux-amd64" % "1.0.392"
        case mac if mac.contains("mac")  => "com.almworks.sqlite4java" % "libsqlite4java-osx" % "1.0.392"
        case osName: String => throw new RuntimeException(s"Unknown operating system $osName")
      }
    ),
    fork := true,
    // TODO: avoid local file copy (send end of buid.sbt) and load directly from .ivy directory
    javaOptions := Seq("-Dsqlite4java.library.path=" + file(".").absolutePath),
  )
  // configure Scala-Native settings
  .nativeSettings( // defined in sbt-scala-native
    // Set to false or remove if you want to show stubs as linking errors
    nativeLinkStubs := true,
    nativeMode := "debug",
    // %%% now include Scala Native. It applies to all selected platforms
    libraryDependencies += "com.github.david-bouyssie" % "sqlite4s_native0.3_2.11" % "0.1.0"
  )

lazy val mzdb4sCoreJVM    = mzdb4sCore.jvm
lazy val mzdb4sCoreNative = mzdb4sCore.native

// TODO: uncomment this when ready for publishing
/*
val publishSettings = Seq(

  // Your profile name of the sonatype account. The default is the same with the organization value
  sonatypeProfileName := "david-bouyssie",

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/mzdb/mzdb4s"),
      "scm:git@github.com:mzdb/mzdb4s.git"
    )
  ),

  developers := List(
    Developer(
      id    = "david-bouyssie",
      name  = "David Bouyssié",
      email = "",
      url   = url("https://github.com/david-bouyssie")
    )
  ),
  description := "",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")), // FIXME: update license
  homepage := Some(url("https://github.com/mzdb/mzdb4s")),
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  // Workaround for issue https://github.com/sbt/sbt/issues/3570
  updateOptions := updateOptions.value.withGigahorse(false),

  useGpg := true,
  pgpPublicRing := file("~/.gnupg/pubring.kbx"),
  pgpSecretRing := file("~/.gnupg/pubring.kbx"),

  Test / skip in publish := true
)*/

/*** Copy SQLite4Java native libraries in current directory (dev mode) ***/
val ivyHome = sys.props.getOrElse("sbt.ivy.home", default = Path.userHome + "/.ivy2" )
val sqlite4javaDir = ivyHome + "/cache/com.almworks.sqlite4java"
val sqlite4javaWin64Lib = file(sqlite4javaDir + "/sqlite4java-win32-x64/dlls/sqlite4java-win32-x64-1.0.392.dll")
//com.almworks.sqlite4java/libsqlite4java-linux-amd64/sos/libsqlite4java-linux-amd64-1.0.392.so
val sqlite4javaLinuxLib = file(sqlite4javaDir + "/libsqlite4java-linux-amd64/sos/libsqlite4java-linux-amd64-1.0.392.so")
val localSqlite4javaWin64Lib = file("./") / "sqlite4java-win32-x64-1.0.392.dll"
val localSqlite4javaLinuxLib = file("./") / "libsqlite4java-linux-amd64-1.0.392.so"
val osName = sys.props("os.name")

val copySqlite4javaWin64Lib = if (osName.startsWith("Windows") && !localSqlite4javaWin64Lib.exists ) IO.copyFile( sqlite4javaWin64Lib, localSqlite4javaWin64Lib )
val copySqlite4javaLinuxLib = if (osName.startsWith("Linux") && !localSqlite4javaLinuxLib.exists ) IO.copyFile( sqlite4javaLinuxLib, localSqlite4javaLinuxLib )

