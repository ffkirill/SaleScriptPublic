import org.scalajs.sbtplugin.impl.DependencyBuilders

enablePlugins(ScalaJSPlugin)
enablePlugins(WorkbenchPlugin)

name := "Scenarist"

//crossScalaVersions := Seq("2.11.8", "2.12.0")
scalaVersion := "2.12.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

emitSourceMaps := true

persistLauncher := true

val diodeVersion = "1.1.0"
val scalaJsReactVersion = "0.11.3"
val uPickleVersion = "0.4.4"
val reactWebJarVersion = "15.3.2"

relativeSourceMaps := true
skip in packageJSDependencies := false

autoCompilerPlugins := true
addCompilerPlugin("tv.cntt" %% "xgettext" % "1.5.0")

scalacOptions ++= Seq(
  "scenarist.i18n", "t:gettext"
).map("-P:xgettext:" + _)

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJsReactVersion,
  "me.chrons" %%% "diode" % diodeVersion,
  "me.chrons" %%% "diode-core" % diodeVersion,
  "me.chrons" %%% "diode-devtools" % diodeVersion,
  "me.chrons" %%% "diode-react" % diodeVersion,
  "com.lihaoyi" %%% "upickle" % uPickleVersion,
  "io.github.soc" %%% "scala-java-time" % "2.0.0-M5"
)

jsDependencies ++= Seq(

  "org.webjars.bower" % "react" % reactWebJarVersion
    /        "react-with-addons.js"
    minified "react-with-addons.min.js"
    commonJSName "React",

  "org.webjars.bower" % "react" % reactWebJarVersion
    /         "react-dom.js"
    minified  "react-dom.min.js"
    dependsOn "react-with-addons.js"
    commonJSName "ReactDOM",

  ProvidedJS / "d3.v3.min.js",
  ProvidedJS / "dagre-d3.min.js",
  ProvidedJS / "graph.js"

)

lazy val copyjs = TaskKey[Unit]("copyjs", "Copy javascript files to target directory")
copyjs := {
  val outDir = baseDirectory.value / "dist/js"
  val inDir = baseDirectory.value / "target/scala-2.12"
  val files = Seq(
    "scenarist-opt.js",
    "scenarist-jsdeps.min.js",
    "scenarist-launcher.js"
  ) map { p =>   (inDir / p, outDir / p) }
  IO.copy(files, overwrite = true)
}

addCommandAlias("buildDist", ";fullOptJS;copyjs")
