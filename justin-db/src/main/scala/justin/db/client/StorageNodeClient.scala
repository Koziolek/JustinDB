package justin.db.client

import java.util.UUID
import justin.db.replication.{R, W}
import scala.concurrent.Future

trait StorageNodeClient {
  def get(id: UUID, r: R): Future[GetValueResponse]
  def write(id: UUID, value: String, w: W): Future[WriteValueResponse]
}

sealed trait GetValueResponse
object GetValueResponse {
  case class Found(value: String) extends GetValueResponse
  case object NotFound extends GetValueResponse
  case class Failure(error: String) extends GetValueResponse
}

sealed trait WriteValueResponse
object WriteValueResponse {
  case object Success extends WriteValueResponse
  case class Failure(error: String) extends WriteValueResponse
}
