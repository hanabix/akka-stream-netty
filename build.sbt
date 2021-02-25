import Dependencies._

def commonSettings(module: String) = Seq(
  name := module,
  organization := "com.github.zhongl",
  scalaVersion := "2.12.12",
  scalafmtOnCompile := true,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8"
  ),
  homepage := Some(url("https://github.com/zhongl/akka-stream-netty")),
  licenses := List(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  developers := List(
    Developer(
      "zhongl",
      "Lunfu Zhong",
      "zhong.lunfu@gmail.com",
      url("https://github.com/zhongl")
    )
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
