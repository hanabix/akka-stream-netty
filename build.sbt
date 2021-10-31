import Dependencies._

inThisBuild(
  Seq(
    scalaVersion       := "2.13.6",
    scalafmtOnCompile  := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8"
    ),
    crossScalaVersions := Seq(scalaVersion.value, "2.12.15"),
    organization       := "com.github.zhongl",
    homepage           := Some(url("https://github.com/hanabix/akka-stream-netty")),
    licenses           := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers         := List(
      Developer(
        "zhongl",
        "Lunfu Zhong",
        "zhong.lunfu@gmail.com",
        url("https://github.com/zhongl")
      )
    )
  )
)

def commonSettings(module: String) = Seq(
  name := module
)

lazy val root = (project in file("."))
  .settings(
    commonSettings("akka-stream-netty"),
    publish / skip := true
  )
  .aggregate(core, epoll, kqueue, all)

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
