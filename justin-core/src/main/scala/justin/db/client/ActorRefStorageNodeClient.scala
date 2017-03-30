package justin.db.client

import java.util.UUID

import akka.pattern.ask
import akka.util.Timeout
import justin.db.replica.W
import justin.db.Data
import justin.db.actors.protocol.{StorageNodeReadData, StorageNodeReadingResult, StorageNodeWriteData, StorageNodeWritingResult}
import justin.db.actors.StorageNodeActorRef
import justin.db.replica.{R, W}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ActorRefStorageNodeClient(private val storageNodeActor: StorageNodeActorRef)(implicit ex: ExecutionContext) extends StorageNodeClient {

  implicit val timeout = Timeout(5.seconds) // TODO: tune this value

  override def get(id: UUID, r: R): Future[GetValueResponse] = {
    lazy val errorMsg = s"[HttpStorageNodeClient] Couldn't read value with id ${id.toString}"

    (storageNodeActor.storageNodeActor ? StorageNodeReadData.Replicated(r, id)).mapTo[StorageNodeReadingResult].map {
      case StorageNodeReadingResult.Found(data)     => GetValueResponse.Found(data)
      case StorageNodeReadingResult.Conflicts(data) => GetValueResponse.Conflicts(data)
      case StorageNodeReadingResult.NotFound        => GetValueResponse.NotFound
      case StorageNodeReadingResult.FailedRead      => GetValueResponse.Failure(errorMsg)
    }.recover { case _                              => GetValueResponse.Failure(errorMsg) }
  }

  override def write(data: Data, w: W): Future[WriteValueResponse] = {
    lazy val errorMsg = s"[HttpStorageNodeClient] Couldn't write data: $data"

    (storageNodeActor.storageNodeActor ? StorageNodeWriteData.Replicate(w, data)).mapTo[StorageNodeWritingResult].map {
      case StorageNodeWritingResult.SuccessfulWrite(id)   => WriteValueResponse.Success
      case StorageNodeWritingResult.ConflictedWrite(_, _) => WriteValueResponse.Conflict
      case StorageNodeWritingResult.FailedWrite           => WriteValueResponse.Failure(errorMsg)
    }.recover { case _                                    => WriteValueResponse.Failure(errorMsg) }
  }
}
