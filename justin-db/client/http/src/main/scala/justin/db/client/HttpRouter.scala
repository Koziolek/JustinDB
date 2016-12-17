package justin.db.client

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import justin.db.Data
import justin.db.client.JustinDirectives._
import justin.db.client.Unmarshallers.UUIDUnmarshaller
import justin.db.replication.{R, W}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

object HttpRouter {
  import Unmarshallers.UuidFormat

  case class Result(value: String)
  implicit val valueFormat = jsonFormat1(Result)

  case class PutValue(id: UUID, value: String, w: Int)
  implicit val putValueFormat = jsonFormat3(PutValue)
}

class HttpRouter(client: HttpStorageNodeClient)(implicit ec: ExecutionContext, mat: Materializer) {
  import HttpRouter._

  def routes: Route = withVectorClockHeader { vClockHeader =>
    (get & path("get") & pathEndOrSingleSlash & parameters('id.as(UUIDUnmarshaller), 'r.as[Int])) { (uuid, r) =>
      complete {
        client.get(uuid, R(r)).map[ToResponseMarshallable] {
          case GetValueResponse.Found(data)              => OK              -> Result(data.value)
          case GetValueResponse.Conflicted(data1, data2) => MultipleChoices -> Result("Multiple Choices") // TODO: finish
          case GetValueResponse.NotFound                 => NotFound        -> Result(s"Not found value with id ${uuid.toString}")
          case GetValueResponse.Failure(err)             => BadRequest      -> Result(err)
        }
      }
    } ~
    (post & path("put") & pathEndOrSingleSlash & entity(as[PutValue])) { putValue =>
      complete {
        client.write(Data(putValue.id, putValue.value, vClockHeader.vectorClock), W(putValue.w)).map[ToResponseMarshallable] {
          case WriteValueResponse.Success      => NoContent
          case WriteValueResponse.Conflict     => MultipleChoices -> Result("Multiple Choices")
          case WriteValueResponse.Failure(err) => BadRequest      -> Result(err)
        }
      }
    }
  }
}
