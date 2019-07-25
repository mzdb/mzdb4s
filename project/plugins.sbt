addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "0.6.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.3.8") // TODO: upgrade to 0.3.9 when sqlite4s released
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                       % "1.1.1")
addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"                  % "2.0")
addSbtPlugin("com.orrsella"       % "sbt-stats"                     % "1.0.7")
// TODO: update to SBT 1.3.0-RC3 in build.properties