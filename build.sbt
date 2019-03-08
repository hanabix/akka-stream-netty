import Dependencies._

def commonSettings(module: String) = Seq(
  name := module,
  organization := "com.github.zhongl.akka-stream-netty",
  version := "0.0.1",
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8"
  )
)

lazy val core = (project in file("core"))
  .settings(
    commonSettings("core"),
    libraryDependencies ++= common ++ akka ++ `netty-`("codec")
  )

lazy val epoll = (project in file("epoll"))
  .settings(
    commonSettings("epoll"),
    libraryDependencies ++= `netty-`("transport-native-epoll")
  )
  .dependsOn(core)

lazy val kqueue = (project in file("kqueue"))
  .settings(
    commonSettings("kqueue"),
    libraryDependencies ++= `netty-`("transport-native-kqueue")
  )
  .dependsOn(core)

lazy val unix = (project in file("unix"))
  .settings(
    commonSettings("unix")
  )
  .dependsOn(epoll, kqueue)
