import sbt._

object Version {
  val scala        = "2.12.2"
  val scalaBinary  = scala.substring(0,4)

  val akka         = "2.4.17"
  val akkaKryo     = "0.5.2"
  val akkaHttp     = "10.0.5"
  val akkaSse      = "2.0.0"
  val scalatest    = "3.0.3"
  val scalacheck   = "1.13.5"
  val sigarLoader  = "1.6.6"
  val scopt        = "3.5.0"
  val logback      = "1.2.3"
  val scalaLogging = "3.5.0"
  val constructR   = "0.17.0"
  val configAnnotation = "0.3.6"
  val macroParadise    = "2.1.0"
}

object Library {
  val akkaActor            = "com.typesafe.akka"     %% "akka-actor"                    % Version.akka
  val akkaSfl4j            = "com.typesafe.akka"     %% "akka-slf4j"                    % Version.akka
  val akkaTestkit          = "com.typesafe.akka"     %% "akka-testkit"                  % Version.akka
  val akkaKryo             = "com.github.romix.akka" %% "akka-kryo-serialization"       % Version.akkaKryo

  // http
  val akkaHttp             = "com.typesafe.akka" %% "akka-http"                         % Version.akkaHttp
  val akkaHttpSprayJson    = "com.typesafe.akka" %% "akka-http-spray-json"              % Version.akkaHttp
  val akkaHttpTestkit      = "com.typesafe.akka" %% "akka-http-testkit"                 % Version.akkaHttp
  val akkaStream           = "com.typesafe.akka" %% "akka-stream"                       % Version.akka

  // cluster
  val akkaRemote           = "com.typesafe.akka" %% "akka-remote"                       % Version.akka
  val akkaCluster          = "com.typesafe.akka" %% "akka-cluster"                      % Version.akka
  val akkaClusterMetrics   = "com.typesafe.akka" %% "akka-cluster-metrics"              % Version.akka
  val akkaClusterTools     = "com.typesafe.akka" %% "akka-cluster-tools"                % Version.akka
  val akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit"           % Version.akka

  // test
  val scalactic            = "org.scalactic"     %% "scalactic"                         % Version.scalatest
  val scalatest            = "org.scalatest"     %% "scalatest"                         % Version.scalatest
  val scalacheck           = "org.scalacheck"    %% "scalacheck"                        % Version.scalacheck

  // other
  val kamonSigar           = "io.kamon"                    % "sigar-loader"             % Version.sigarLoader
  val scopt                = "com.github.scopt"           %% "scopt"                    % Version.scopt
  val logback              = "ch.qos.logback"              % "logback-classic"          % Version.logback
  val scalaLogging         = "com.typesafe.scala-logging" %% "scala-logging"            % Version.scalaLogging
  val configAnnotation     = "com.wacai"                  %% "config-annotation"        % Version.configAnnotation
  val macroParadise        = "org.scalamacros"            % "paradise"                  % Version.macroParadise

  // server side events
  val akkaSse              = "de.heikoseeberger" %% "akka-sse"                          % Version.akkaSse

  val constructR           = "de.heikoseeberger" %% "constructr"                        % Version.constructR
  val constructRetcd       = "de.heikoseeberger" %% "constructr-coordination-etcd"      % Version.constructR
}

object Dependencies {
  import Library._

  private val genericTest = Seq(scalactic, scalatest % "test")

  private val akkaCommon        = Seq(akkaActor, akkaSfl4j, akkaTestkit, akkaKryo)
  private val akkaHttpCommon    = Seq(akkaHttp, akkaHttpSprayJson, akkaHttpTestkit, akkaStream)
  private val akkaClusterCommon = Seq(akkaRemote, akkaMultiNodeTestkit % "multi-jvm", akkaCluster, akkaClusterMetrics, akkaClusterTools, kamonSigar)

  private val constructr = Seq(constructR, constructRetcd)

  val core = akkaCommon ++ akkaClusterCommon ++ genericTest ++ Seq(scalacheck % "test", logback, scalaLogging) ++ Seq(akkaHttpSprayJson)

  val httpApi = akkaCommon ++ akkaHttpCommon ++ genericTest ++ Seq(scopt) ++ Seq(akkaSse)

  val storageApi = genericTest
  val storageInMem = genericTest
  val storagePersistent = genericTest

  val root = core ++ httpApi ++ storageApi ++ constructr
}
