import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

Global / onChangedBuildSource := IgnoreSourceChanges

inThisBuild(
  Seq(
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.6",
  ),
)

lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full),

  resolvers ++=
    ("jitpack" at "https://jitpack.io") ::
      Nil,

  libraryDependencies ++=
    Deps.scalatest.value % Test ::
      Nil,

  /* scalacOptions --= Seq("-Xfatal-warnings"), */
)

lazy val jsSettings = Seq(
  useYarn := true,
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  webpack / version := "4.46.0",
  Compile / npmDevDependencies += NpmDeps.funpack,
  Compile / npmDevDependencies ++= NpmDeps.Dev,
)

lazy val webSettings = Seq(
  scalaJSUseMainModuleInitializer := true,
  Test / requireJsDomEnv := true,
  startWebpackDevServer / version := "3.11.2",
  webpackDevServerExtraArgs := Seq("--color"),
  webpackDevServerPort := 12345,
  fastOptJS / webpackConfigFile := Some(
    baseDirectory.value / "webpack.config.dev.js",
  ),
  fullOptJS / webpackConfigFile := Some(
    baseDirectory.value / "webpack.config.prod.js",
  ),
  fastOptJS / webpackBundlingMode := BundlingMode.LibraryOnly(),
  libraryDependencies += Deps.portableScala.value,
)

lazy val webClient = project
  .enablePlugins(
    ScalaJSPlugin,
    ScalaJSBundlerPlugin,
    ScalablyTypedConverterPlugin,
  )
  .dependsOn(apiHttp.js)
  .in(file("web-client"))
  .settings(commonSettings, jsSettings, webSettings)
  .settings(
    fullOptJS / webpackEmitSourceMaps := false,
    libraryDependencies ++=
      Deps.outwatch.core.value ::
      Deps.funstack.web.value ::
      Deps.colibri.router.value ::
      Nil,
    Compile / npmDependencies ++=
      NpmDeps.tailwindForms ::
        NpmDeps.tailwindTypography ::
        NpmDeps.snabbdom ::
        Nil,
    stIgnore ++=
      "@tailwindcss/forms" ::
        "@tailwindcss/typography" ::
        "snabbdom" ::
        Nil,
  )

lazy val apiHttp = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("api-http"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Deps.tapir.core.value ::
        Deps.tapir.jsonCirce.value ::
        Nil,
  )

lazy val lambdaHttp = project
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterPlugin,
  )
  .in(file("lambda-http"))
  .settings(commonSettings, jsSettings)
  .dependsOn(apiHttp.js)
  .settings(
    /* scalaJSLinkerConfig ~= { _.withOptimizer(false) }, */
    webpackEmitSourceMaps in fullOptJS := false,
    webpackConfigFile in fullOptJS := Some(
      baseDirectory.value / "webpack.config.prod.js",
    ),
    libraryDependencies ++=
      Deps.funstack.lambdaHttp.value ::
        Nil,
  )

addCommandAlias("dev", "devInit; devWatchAll; devDestroy") // watch all
addCommandAlias("devInit", "webClient/fastOptJS::startWebpackDevServer")
addCommandAlias("devWatchAll", "~; webClient/fastOptJS::webpack")
addCommandAlias("devDestroy", "webClient/fastOptJS::stopWebpackDevServer")
