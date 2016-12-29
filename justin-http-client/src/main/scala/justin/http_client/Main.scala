package justin.http_client

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import justin.consistent_hashing.{NodeId, Ring}
import justin.db.client.ActorRefStorageNodeClient
import justin.db.replication.N
import justin.db.storage.InMemStorage
import justin.db.{StorageNodeActor, StorageNodeActorRef}

object Main extends App {
  val config = ConfigFactory.parseString(s"akka.cluster.roles = [${StorageNodeActor.role}]")
    .withFallback(ConfigFactory.load())

  implicit val system       = ActorSystem("justin-db-cluster-system", config)
  implicit val executor     = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  logger.info("JustinDB " + BuildInfo.version)
  logger.info("Build Info: ")
  logger.info(BuildInfo.toString)

  Cluster(system).registerOnMemberUp {
    logger.info("Cluster is ready!")

    // STORAGE ACTOR
    val storageNodeActorRef = StorageNodeActorRef {
      val nodeId      = NodeId(config.getInt("node.id"))
      val ring        = Ring(config.getInt("ring.cluster-nodes-size"), config.getInt("ring.creation-size"))
      val storage     = new InMemStorage()
      val replication = N(config.getInt("justin-db.replication.N"))

      system.actorOf(
        props = StorageNodeActor.props(nodeId, storage, ring, replication),
        name  = StorageNodeActor.name(nodeId)
      )
    }

    // HTTP API
    val routes = logRequestResult(system.name) {
      new HttpRouter(new ActorRefStorageNodeClient(storageNodeActorRef)).routes ~
        new HealthCheckRouter().routes ~
        new BuildInfoRouter().routes
    }

    Http()
      .bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
      .map { binding => logger.info(s"HTTP server started at ${binding.localAddress}") }
      .recover { case ex => logger.error(ex, "Could not start HTTP server") }
  }
}
