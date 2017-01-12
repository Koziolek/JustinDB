package justin.db

import java.util.UUID

import justin.db.StorageNodeActorProtocol.StorageNodeReadingResult
import justin.db.storage.PluggableStorageProtocol
import justin.db.storage.PluggableStorageProtocol.{Ack, DataOriginality, StorageGetData, StoragePutData}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReplicaLocalReaderTest extends FlatSpec with Matchers with ScalaFutures {

  behavior of "Replica Local Reader"

  it should "found data for existing key" in {
    // given
    val id   = UUID.randomUUID()
    val data = Data(id, "value")
    val service = new ReplicaLocalReader(new PluggableStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality): Future[StorageGetData] = {
        Future.successful(StorageGetData.Single(data))
      }

      override def put(cmd: StoragePutData)(resolveOriginality: (UUID) => DataOriginality): Future[Ack] = ???
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.Found(data) }
  }

  it should "not found data for non-existing key" in {
    // given
    val id = UUID.randomUUID()
    val service = new ReplicaLocalReader(new PluggableStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality): Future[StorageGetData] = {
        Future.successful(StorageGetData.None)
      }
      override def put(cmd: StoragePutData)(resolveOriginality: (UUID) => DataOriginality): Future[Ack] = ???
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.NotFound }
  }

  it should "found conflicted data for existing key" in {
    // given
    val id = UUID.randomUUID()
    val data1 = Data(id, "some-data-1")
    val data2 = Data(id, "some-data-2")
    val service = new ReplicaLocalReader(new PluggableStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality): Future[StorageGetData] = {
        Future.successful(StorageGetData.Conflicted(data1, data2))
      }
      override def put(cmd: StoragePutData)(resolveOriginality: (UUID) => DataOriginality): Future[Ack] = ???
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.Conflicted(data1, data2) }
  }

  it should "recover failure reading" in {
    // given
    val id = UUID.randomUUID()
    val service = new ReplicaLocalReader(new PluggableStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality): Future[StorageGetData] = Future.failed(new Exception)
      override def put(cmd: StoragePutData)(resolveOriginality: (UUID) => DataOriginality): Future[Ack] = ???
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.FailedRead }
  }
}
