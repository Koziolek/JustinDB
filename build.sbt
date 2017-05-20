name           := "JustinDB"
version        := "0.3"
maintainer     := "Mateusz Maciaszek"
packageSummary := "JustinDB"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

fork in run := true

scalacOptions := Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Xfatal-warnings",
  "-encoding",
  "utf8",
  "-language:implicitConversions"
)

// Force building with Java 8
initialize := {
  val required = "1.8"
  val current = sys.props("java.specification.version")
  assert(current == required, s"Unsupported build JDK: java.specification.version $current != $required")
}

lazy val configAnnotationSettings: Seq[sbt.Setting[_]] = {
  Seq(
    scalacOptions += "-Xmacro-settings:conf.output.dir=" + baseDirectory.value.getAbsolutePath + "/src/main/resources",
    addCompilerPlugin(Library.macroParadise cross CrossVersion.full),
    libraryDependencies += Library.configAnnotation
  )
}

// PROJECT DEFINITIONS
lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, SbtMultiJvm)
  .configs(MultiJvm)
  .settings(
    mainClass in assembly := Some("justin.Main"),
    test in assembly := {},
    libraryDependencies ++= Dependencies.root,
    scalaVersion := Version.scala,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitHeadCommit, git.gitCurrentBranch),
    buildInfoOptions += BuildInfoOption.ToJson
  )
  .settings(versionWithGit)
  .settings(git.useGitDescribe := true)
  .settings(configAnnotationSettings)
  .aggregate(core, httpApi, storageInMem, storagePersistent)
  .dependsOn(core, httpApi, storageInMem, storagePersistent) // TODO: storageInMem/storagePersistent should be provided

lazy val core = (project in file("justin-core"))
  .enablePlugins(SbtMultiJvm)
  .configs(MultiJvm)
  .settings(
    name := "justin-core",
    scalaVersion := Version.scala,
    libraryDependencies ++= Dependencies.core
  )
  .aggregate(merkleTrees, vectorClocks, consistentHashing, crdts, storageAPi)
  .dependsOn(merkleTrees, vectorClocks, consistentHashing, crdts, storageAPi)

lazy val httpApi = (project in file("justin-http-api"))
  .settings(
    name := "justin-http-api",
    scalaVersion := Version.scala,
    libraryDependencies ++= Dependencies.httpClient,
    fork in Test := true,
    javaOptions in Test += "-Dconfig.resource=test.conf"
  )
  .dependsOn(core)

lazy val storageAPi = (project in file("justin-storage-api")).settings(
  name := "justin-storage-api",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.storageApi
)

lazy val storageInMem = (project in file("justin-storage-in-mem")).settings(
  name := "justin-storage-in-mem",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.storageInMem
).dependsOn(storageAPi)

lazy val storagePersistent = (project in file("justin-storage-persistent")).settings(
  name := "justin-storage-persistent",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.storagePersistent
).dependsOn(storageAPi)

lazy val merkleTrees = (project in file("justin-merkle-trees")).settings(
  name := "justin-merkle-trees",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.merkleTrees
)

lazy val vectorClocks = (project in file("justin-vector-clocks")).settings(
  name := "justin-vector-clocks",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.vectorClocks
)

lazy val crdts = (project in file("justin-crdts")).settings(
  name := "justin-crdts",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.crdts
)

lazy val consistentHashing = (project in file("justin-consistent-hashing")).settings(
  name := "justin-consistent-hashing",
  scalaVersion := Version.scala,
  libraryDependencies ++= Dependencies.consistenHashing
)

// ALIASES
addCommandAlias("compileAll", ";compile;test:compile;multi-jvm:compile")
