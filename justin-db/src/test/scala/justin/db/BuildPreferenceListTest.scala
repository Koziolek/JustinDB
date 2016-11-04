package justin.db

import org.scalatest.{FlatSpec, Matchers}

class BuildPreferenceListTest extends FlatSpec with Matchers {

  behavior of "Builder of Preference List"

  it should "build preference list" in {
    val nodeId = StorageNodeActorId(1)
    val replicationFactor = ReplicationFactor(3)

    val list = BuildPreferenceList(nodeId, replicationFactor)

    list shouldBe List(StorageNodeActorId(2), StorageNodeActorId(3), StorageNodeActorId(4))
    list.contains(nodeId) shouldBe false
  }

  it should "build empty preference list when factor replication is 0" in {
    val nodeId = StorageNodeActorId(1)
    val replicationFactor = ReplicationFactor(0)

    val list = BuildPreferenceList(nodeId, replicationFactor)

    list shouldBe List.empty[StorageNodeActorId]
    list.contains(nodeId) shouldBe false
  }
}
