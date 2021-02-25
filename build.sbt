import Dependencies._

def commonSettings(module: String) = Seq(
  name := module,
  organization := "com.github.zhongl",
  version := "0.0.1",
  scalaVersion := "2.12.8",
  scalafmtOnCompile := true,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8"
  )
)

lazy val core = (project in file("core"))
  .settings(
    commonSettings("akka-stream-netty-core"),
    libraryDependencies ++= common ++ akka ++ `netty-`("codec")
  )

lazy val epoll = (project in file("epoll"))
  .settings(
    commonSettings("akka-stream-netty-epoll"),
    libraryDependencies ++= `netty-`("transport-native-epoll").map(_.classifier("linux-x86_64"))
  )
  .dependsOn(core)

lazy val kqueue = (project in file("kqueue"))
  .settings(
    commonSettings("akka-stream-netty-kqueue"),
    libraryDependencies ++= `netty-`("transport-native-kqueue").map(_.classifier("osx-x86_64"))
  )
  .dependsOn(core)

lazy val all = (project in file("all"))
  .settings(
    commonSettings("akka-stream-netty-all"),
    libraryDependencies ++= common ++ akka
  )
  .dependsOn(epoll, kqueue)
