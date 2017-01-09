package justin.http_client

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import justin.db.entropy.ActiveAntiEntropyActorRef

import scala.concurrent.ExecutionContext

class ActiveAntiEntropyRouter(actorRef: ActiveAntiEntropyActorRef)(implicit ec: ExecutionContext, mat: Materializer) {

  val routes: Route = path("aae" / "run") {
    post { complete(StatusCodes.NoContent) }
  }
}
