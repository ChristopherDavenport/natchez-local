ThisBuild / tlBaseVersion := "0.2" // your current series x.y

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)
ThisBuild / tlCiReleaseBranches := Seq("main")
ThisBuild / tlSonatypeUseLegacyHost := true


val Scala213 = "2.13.14"

ThisBuild / crossScalaVersions := Seq("2.12.18", Scala213, "3.3.0")
ThisBuild / scalaVersion := Scala213

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

val catsV = "2.9.0"
val catsEffectV = "3.5.0"
val munitCatsEffectV = "1.0.7"


// Projects
lazy val `natchez-local` = tlCrossRootProject
  .aggregate(core)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "natchez-local",

    libraryDependencies ++= Seq(
      "org.typelevel"               %%% "cats-core"                  % catsV,
      "org.typelevel"               %%% "cats-effect"                % catsEffectV,
      "org.tpolecat"                %%% "natchez-core"               % "0.3.2",
      "io.chrisdavenport"           %%% "fiberlocal"                 % "0.1.1",
      "org.typelevel"               %%% "munit-cats-effect-3"        % munitCatsEffectV         % Test,

    )
  ).jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule)},
  )

lazy val site = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm)
