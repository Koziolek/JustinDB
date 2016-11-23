package justin.db.client

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import justin.db.StorageNodeActorRef
import justin.db.replication.R
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

import scala.concurrent.Future

class StorageNodeRouterTest extends FlatSpec with Matchers with ScalatestRouteTest {

  behavior of "Storage Node Router"

  it should "get OK result for specific id and r" in {
    val value  = "value"
    val id     = UUID.randomUUID().toString
    val r      = 1
    val router = new StorageNodeRouter(getFound(value))

    Get(s"/get?id=$id&r=$r") ~> Route.seal(router.routes) ~> check {
      status                       shouldBe StatusCodes.OK
      responseAs[String].parseJson shouldBe JsObject("value" -> JsString(value))
    }
  }

  private def getFound(value: String) = new HttpStorageNodeClient(StorageNodeActorRef(null)) {
    override def get(id: UUID, r: R): Future[GetValueResponse] = Future.successful(GetValueResponse.Found(value))
  }
}
