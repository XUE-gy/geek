val scalaV = "2.12.10"


val projectName = "geek"
val projectVersion = "2020.01.09"

resolvers += Resolver.sonatypeRepo("snapshots")


val projectMainClass = "org.seekloud.geek.Boot"
val clientMain = "org.seekloud.geek.CLientBoot"

def commonSettings = Seq(
  version := projectVersion,
  scalaVersion := scalaV,
  scalacOptions ++= Seq(
    //"-deprecation",
    "-feature"
  ),
  javacOptions ++= Seq("-encoding", "UTF-8")
)

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(commonSettings: _*)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// Scala-Js frontend
lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "frontend")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //mainClass := Some("com.neo.sk.virgour.front.Main"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % Dependencies.circeVersion,
      "io.circe" %%% "circe-generic" % Dependencies.circeVersion,
      "io.circe" %%% "circe-parser" % Dependencies.circeVersion,
      "org.scala-js" %%% "scalajs-dom" % Dependencies.scalaJsDomV,
      //"io.suzaku" %%% "diode" % "1.1.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.7" withSources(),
      // "com.github.japgolly.scalacss" %%% "core" % "0.5.5" withSources(),
      "in.nvilla" %%% "monadic-html" % "0.4.0" withSources()
    )
  )
  .dependsOn(sharedJs)



//lazy val client = (project in file("client")).enablePlugins(PackPlugin)
//  .settings(commonSettings: _*)
//  .settings(
//    mainClass in reStart := Some(clientMain),
//    javaOptions in reStart += "-Xmx2g"
//  )
//  .settings(name := "client")
//  .settings(
//    //pack
//    // If you need to specify main classes manually, use packSettings and packMain
//    //packSettings,
//    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
//    packMain := Map("client" -> clientMain),
//    packJvmOpts := Map("client" -> Seq("-Xmx4096m", "-Xms128m")),
//    packExtraClasspath := Map("client" -> Seq("."))
//  )
//  .settings(
//    //    libraryDependencies ++= Dependencies.backendDependencies,
//    libraryDependencies ++= Dependencies.bytedecoLibs,
//    libraryDependencies ++= Dependencies4Client.clientDependencies,
//  )
//  .dependsOn(sharedJvm)


// Akka Http based backend
lazy val backend = (project in file("backend")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(projectMainClass),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "backend")

  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("geek" -> projectMainClass),
    packJvmOpts := Map("geek" -> Seq("-Xmx512m", "-Xms1024m")),
    packExtraClasspath := Map("geek" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  )
  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in frontend).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in frontend).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in frontend).value,
    watchSources ++= (watchSources in frontend).value
  )
  .dependsOn(sharedJvm)


lazy val root = (project in file("."))
  .aggregate(frontend,backend)
  .settings(name := projectName)