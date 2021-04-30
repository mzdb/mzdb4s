import scala.language.postfixOps
import scala.sys.process._

lazy val scala213 = "2.13.5"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala211, scala213)

val sharedSettings = Seq(
  organization := "com.github.mzdb",
  version := "0.4.0",
  scalaVersion := scala213,
  crossScalaVersions := supportedScalaVersions,

  libraryDependencies ++= Seq(
    "com.outr" %%% "scribe" % "3.3.3",
    "com.lihaoyi" %%% "utest" % "0.7.7" % Test
  ),

  testFrameworks += new TestFramework("utest.runner.Framework")
)

val sharedJvmSettings = Seq(
  fork := true,
  // TODO: avoid local file copy (see end of buid.sbt) and load directly from .ivy directory
  javaOptions := Seq(
    "-Dsqlite4java.library.path=" + file(".").absolutePath,
    "-Xmx4G"
  ),
  resolvers ++= Seq(
    "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
  )
)

val sharedNativeSettings = Seq(
  // Set to false or remove if you want to show stubs as linking errors
  nativeLinkStubs := true,
  nativeMode := "debug", //"debug", //"release-fast", //"release",
  nativeLTO := "none",   // "none","thin" // note: thin doesn't work when Rust static libraries are linked
  nativeGC := "immix"    // "none","boehm","commix","immix"
)

/*
.settings(
    Compile / nativeConfig ~= { _.withLTO(LTO.thin).withMode(Mode.releaseFast) },
    Test / nativeConfig ~= { _.withLTO(LTO.none).withMode(Mode.debug) }
  )
 */

lazy val makeLibraries = taskKey[Unit]("Building native components")

lazy val mzdb4sCore = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("core"))
  .settings(sharedSettings ++ Seq(name := "mzdb4s-core"))
  .jvmSettings(
    sharedJvmSettings ++ Seq(
      libraryDependencies ++= Seq(
        "com.github.jnr" % "jffi" % "1.3.2",
        "com.github.jnr" % "jnr-ffi" % "2.2.2",
        "com.almworks.sqlite4java" % "sqlite4java" % "1.0.392",

        // OS dependant SQLite library dependency
        // See: https://stackoverflow.com/questions/55591045/sbt-dynamically-detect-building-platform
        sys.props("os.name").toLowerCase match {
          case win if win.contains("win") => "com.almworks.sqlite4java" % "sqlite4java-win32-x64" % "1.0.392"
          case linux if linux.contains("linux") => "com.almworks.sqlite4java" % "libsqlite4java-linux-amd64" % "1.0.392"
          case mac if mac.contains("mac")  => "com.almworks.sqlite4java" % "libsqlite4java-osx" % "1.0.392"
          case osName: String => throw new RuntimeException(s"Unknown operating system $osName")
        }
      )
    )
  )
  // Configure Scala-Native settings
  .nativeSettings(

    sharedNativeSettings ++ Seq(
      nativeMode := "debug",

      libraryDependencies ++= Seq(
        "com.github.david-bouyssie" %%% "sqlite4s" % "0.4.0"
      )
    ),

    nativeLinkingOptions ++= Seq(
      "-L" ++ baseDirectory.value.getAbsolutePath() ++ "/nativelib"
    )
  )

lazy val mzdb4sCore_JVM    = mzdb4sCore.jvm
lazy val mzdb4sCore_Native = mzdb4sCore.native

lazy val mzdb4sIO = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("io"))
  .settings( sharedSettings ++ Seq(name := "mzdb4s-io") )
  .dependsOn( mzdb4sCore )
  .jvmSettings( sharedJvmSettings )
  // Configure Scala-Native settings
  .nativeSettings(

    sharedNativeSettings ++ Seq(

      /*libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "utest" % "0.7.7" % Test
      ),*/

      nativeMode := "release-fast",

      // Link custom native libraries
      nativeLinkingOptions ++= Seq(
        "-L" ++ baseDirectory.in(mzdb4sCore_Native).value.getAbsolutePath() ++ "/nativelib",
        "-L" ++ baseDirectory.value.getAbsolutePath() ++ "/nativelib",
      ),

      // Replace the default "lib" folder by "jars", so that we can use "lib" for native libraries
      //unmanagedBase := baseDirectory.value / "jars",

      // Add the lib folder to the cleanFiles list (issue: the lib folder itself is deleted)
      // See: https://stackoverflow.com/questions/10471596/add-additional-directory-to-clean-task-in-sbt-build
      cleanFiles += baseDirectory.value / "nativelib",

      // Configure the task which will build native libraries from C source code
      // See:
      // - https://binx.io/blog/2018/12/08/the-scala-build-tool/
      // - https://stackoverflow.com/questions/24996437/how-to-execute-a-bash-script-as-sbt-task/25005651
      makeLibraries := {

        val s: TaskStreams = streams.value

        // Create lib directory
        val libDir = (baseDirectory.value / "nativelib")
        libDir.mkdir()

        s.log.info("Building native libraries...")
        // TODO: we may include SQLite as well (https://github.com/azadkuh/sqlite-amalgamation)
        val base64Build: Int = if (libDir / "libbase64.so" exists()) 0 else "make --print-directory --directory=./io/native/c/base64/" !<
        val strBuilderBuild: Int = if (libDir / "libstrbuilder.so" exists()) 0 else "make --print-directory --directory=./io/native/c/strbuilder/" !<
        val yxmlBuild: Int = if (libDir / "libyxml.so" exists()) 0 else "make --print-directory --directory=./io/native/c/yxml/" !<

        if(base64Build == 0 && strBuilderBuild == 0 && yxmlBuild == 0) {
          s.log.success("Native libraries were successfully built!")
        } else {
          throw new IllegalStateException("can't build native libraries!")
        }
      },

      (compile in Compile) := ((compile in Compile) dependsOn makeLibraries).value
    )
  )

lazy val mzdb4sIO_JVM    = mzdb4sIO.jvm
lazy val mzdb4sIO_Native = mzdb4sIO.native


lazy val mzdb4sThermo = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("io-thermo"))
  .settings( sharedSettings ++ Seq(name := "mzdb4s-io-thermo") )
  .dependsOn( mzdb4sIO )
  .jvmSettings( sharedJvmSettings ++ Seq(
    unmanagedBase := baseDirectory.value / "lib" / "windows"

    /*libraryDependencies ++= Seq(
      //"thermorawfileparser" % "thermorawfileparser" % "1.2.3" from "file:///" + baseDirectory.value.getAbsolutePath ++ "/lib/windows/ThermoRawFileParser.jar",
      "com.lihaoyi" %%% "utest" % "0.7.7" % Test
    )*/
  ))
  // Configure Scala-Native settings
  .nativeSettings(
    sharedNativeSettings ++ Seq(

      /*libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "utest" % "0.7.4" % Test
      )*/

      //mainClass in Compile := Some("ThermoToMzDb"),

      nativeMode := "debug", //"debug", //"release-fast"

      // Replace the default "lib" folder by "jars", so that we can use "lib" for native libraries
      //unmanagedBase := baseDirectory.value / "jars",

      // Link custom native libraries
      // FIXME: on Linux we also have to do before execution
      // export LD_LIBRARY_PATH=/mnt/d/Dev/wsl/scala-native/mzdb4s/io-thermo/native/nativelib/
      nativeLinkingOptions ++= Seq(
        "-L" ++ baseDirectory.in(mzdb4sCore_Native).value.getAbsolutePath() ++ "/nativelib",
        "-L" ++ baseDirectory.value.getAbsolutePath() ++ "/nativelib"
      )
    )
  )

lazy val mzdb4sThermo_JVM    = mzdb4sThermo.jvm
lazy val mzdb4sThermo_Native = mzdb4sThermo.native

lazy val mzdb4sTimsData = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("io-timsdata"))
  .settings( sharedSettings ++ Seq(name := "mzdb4s-io-timsdata") )
  .dependsOn( mzdb4sIO )
  /*.jvmSettings( sharedJvmSettings ++ Seq(
    libraryDependencies += "com.github.jnr" % "jnr-ffi" % "2.2.2"
  ))*/
  // Configure Scala-Native settings
  .nativeSettings(
    sharedNativeSettings ++ Seq(
      /*libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "utest" % "0.7.4" % Test
      )*/

      //mainClass in Compile := Some("ThermoToMzDb"),

      nativeMode := "release-fast", //"debug", //"release-fast"

      // Replace the default "lib" folder by "jars", so that we can use "lib" for native libraries
      //unmanagedBase := baseDirectory.value / "jars",

      // Link custom native libraries
      // FIXME: on Linux we also have to do before execution
      // export LD_LIBRARY_PATH=/mnt/d/Dev/wsl/scala-native/mzdb4s/io-timsdata/native/nativelib
      nativeLinkingOptions ++= Seq(
        "-L" ++ baseDirectory.value.getAbsolutePath() ++ "/nativelib"
      )
    )
  )

lazy val mzdb4sTimsData_JVM    = mzdb4sTimsData.jvm
lazy val mzdb4sTimsData_Native = mzdb4sTimsData.native

/*
lazy val mzdb4sProcessing = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("processing"))
  .settings(sharedSettings ++ Seq(
    name := "mzdb4s-processing",
    libraryDependencies ++= Seq("io.suzaku" %%% "boopickle" % "1.3.1")
  ))
  .dependsOn( mzdb4sCore )
  .jvmSettings(
    libraryDependencies ++= Seq(
      // TODO: share with core
      /*"com.almworks.sqlite4java" % "sqlite4java" % "1.0.392",
      // OS dependant SQLite library dependency
      // See: https://stackoverflow.com/questions/55591045/sbt-dynamically-detect-building-platform
      sys.props("os.name").toLowerCase match {
        case win if win.contains("win") => "com.almworks.sqlite4java" % "sqlite4java-win32-x64" % "1.0.392"
        case linux if linux.contains("linux") => "com.almworks.sqlite4java" % "libsqlite4java-linux-amd64" % "1.0.392"
        case mac if mac.contains("mac")  => "com.almworks.sqlite4java" % "libsqlite4java-osx" % "1.0.392"
        case osName: String => throw new RuntimeException(s"Unknown operating system $osName")
      },*/

      "org.msgpack" % "jackson-dataformat-msgpack" % "0.8.18",
      // TODO: stay synchronized with "jackson-dataformat-msgpack" used version
      //"org.apache.commons" % "commons-math3" % "3.6.1",

      // TODO: remove me when SN deps are available for SN 0.4.x
      "biz.enef" %%% "slogging" % "0.6.1",
      "com.lihaoyi" %%% "utest" % "0.6.7+8-7c499c15" % "test"
    ),
    fork := true,
    // TODO: avoid local file copy (see end of buid.sbt) and load directly from .ivy directory
    javaOptions := Seq("-Dsqlite4java.library.path=" + file(".").absolutePath)
  )
  // configure Scala-Native settings
  .nativeSettings( // defined in sbt-scala-native
    // Set to false or remove if you want to show stubs as linking errors
    nativeLinkStubs := true,
    nativeMode := "debug", //"release-fast", //"release",
    // %%% now include Scala Native. It applies to all selected platforms
    //libraryDependencies += "com.github.david-bouyssie" % "sqlite4s_native0.4.0-M2_2.11" % "0.2.0",

    // TODO: remove me when SN deps are available for SN 0.4.x
    libraryDependencies ++= Seq(
      //"tech.sparse" %%% "pine" % "0.1.7-SNAPSHOT", // TODO: publishLocal with version 0.1.6
      "biz.enef" %%% "slogging" % "0.6.2-SNAPSHOT", // TODO: publishLocal with version 0.6.1
      "com.lihaoyi" %%% "utest" % "0.6.7+8-7c499c15" % "test"
    )
  )

lazy val mzdb4sProcessing_JVM    = mzdb4sProcessing.jvm
lazy val mzdb4sProcessing_Native = mzdb4sProcessing.native
*/

lazy val mzdb4sTools = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("tools"))
  .settings( sharedSettings ++ Seq(
    name := "mzdb4s-tools",
    crossScalaVersions := List(scala213),

    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "mainargs" % "0.2.1"
    ),

    // -- SBT assembly settings -- //
    mainClass in assembly := Some("MzDbTools"),
    assemblyJarName in assembly := "mzdbtools.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  ))
  .dependsOn( mzdb4sThermo )
  .dependsOn( mzdb4sTimsData )
  .nativeSettings(
    sharedNativeSettings ++ Seq(

      nativeMode := "debug", //"debug", //"release-fast"

      // Link custom native libraries
      // FIXME: on Linux we also have to do before execution
      // export LD_LIBRARY_PATH=/mnt/d/Dev/wsl/scala-native/mzdb4s/io-thermo/native/nativelib/:/mnt/d/Dev/wsl/scala-native/mzdb4s/io-timsdata/native/nativelib
      nativeLinkingOptions ++= Seq(
        "-L" ++ baseDirectory.in(mzdb4sCore_Native).value.getAbsolutePath() ++ "/nativelib",
        "-L" ++ baseDirectory.in(mzdb4sIO_Native).value.getAbsolutePath() ++ "/nativelib",
        "-L" ++ baseDirectory.in(mzdb4sThermo_Native).value.getAbsolutePath() ++ "/nativelib",
        "-L" ++ baseDirectory.in(mzdb4sTimsData_Native).value.getAbsolutePath() ++ "/nativelib",
        "-Wl,-allow-multiple-definition"
      )
    )
  )

lazy val mzdb4sTools_JVM    = mzdb4sTools.jvm
lazy val mzdb4sTools_Native = mzdb4sTools.native

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

lazy val sqlite4javaWin64Lib = file(sqlite4javaDir + "/sqlite4java-win32-x64/dlls/sqlite4java-win32-x64-1.0.392.dll")
lazy val sqlite4javaLinuxLib = file(sqlite4javaDir + "/libsqlite4java-linux-amd64/sos/libsqlite4java-linux-amd64-1.0.392.so")

lazy val localSqlite4javaWin64Lib = file("./") / "sqlite4java-win32-x64-1.0.392.dll"
//lazy val localSqlite4javaLinuxDir = file("./") //file("/usr/java/packages/lib/amd64")
lazy val localSqlite4javaLinuxLib = file("./") / "libsqlite4java-linux-amd64-1.0.392.so"

val osName = sys.props("os.name")
val copySqlite4javaWin64Lib = if (osName.startsWith("Windows") && !localSqlite4javaWin64Lib.exists ) IO.copyFile( sqlite4javaWin64Lib, localSqlite4javaWin64Lib )
val copySqlite4javaLinuxLib = if (osName.startsWith("Linux") && !localSqlite4javaLinuxLib.exists) IO.copyFile( sqlite4javaLinuxLib, localSqlite4javaLinuxLib )
/*{
  //file("/usr/java/packages/lib/amd64").mkdirs()
  System.setProperty("sqlite4java.library.path", "./")
  IO.copyFile( sqlite4javaLinuxLib, localSqlite4javaLinuxLib )
}*/

