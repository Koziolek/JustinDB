package justin.db.replica

import justin.consistent_hashing.{NodeId, Ring}
import org.scalatest.{FlatSpec, Matchers}

class PreferenceListTest extends FlatSpec with Matchers {

  behavior of "Preference List"

  it should "has size of defined nr of replicas" in {
    // given
    val n = N(3) // nr of replicas
    val ring = Ring(nodesSize = 5, partitionsSize = 64)
    val basePartitionId = 1

    // when
    val preferenceList = PreferenceList(basePartitionId, n, ring).right.get

    // then
    preferenceList.size shouldBe 3
  }

  it should "has defined first node in the list to be the one taken from Ring with initial partitionId" in {
    // given
    val n = N(3) // nr of replicas
    val ring = Ring(nodesSize = 5, partitionsSize = 64)
    val initialPartitionId = 1

    // when
    val coordinator = PreferenceList.apply(initialPartitionId, n, ring).right.get.primaryNodeId

    // then
    coordinator shouldBe ring.getNodeId(initialPartitionId).get
  }

  it should "has at least one member (coordinator) for none replicas" in {
    // given
    val n = N(0) // nr of replicas
    val ring = Ring(nodesSize = 5, partitionsSize = 64)
    val initialPartitionId = 1

    // when
    val preferenceList = PreferenceList.apply(initialPartitionId, n, ring).right.get

    // then
    preferenceList.size          shouldBe 1
    preferenceList.primaryNodeId shouldBe ring.getNodeId(initialPartitionId).get
  }

  it should "check that selected nodes ids are continuous" in {
    // given
    val n = N(3) // nr of replicas
    val ring = Ring(nodesSize = 5, partitionsSize = 64)
    val initialPartitionId = 1

    // when
    val preferenceList = PreferenceList.apply(initialPartitionId, n, ring).right.get

    // then
    preferenceList shouldBe PreferenceList(NodeId(1), List(NodeId(2), NodeId(3)))
  }

  it should "fail to build PreferenceList if coordinator nodeId couldn't be found" in {
    // given
    val n = N(3) // nr of replicas
    val ring = Ring(nodesSize = 5, partitionsSize = 64)
    val notExistedPartitionId = -1

    // when
    val preferenceList = PreferenceList.apply(notExistedPartitionId, n, ring)

    // then
    preferenceList shouldBe Left(PreferenceList.LackOfCoordinator)
  }

  it should "fail to build PreferenceList if it has NOT expected size" in {
    // given
    val n = N(3) // nr of replicas
    val ring = Ring(nodesSize = 2, partitionsSize = 64)
    val initialPartitionId = 1

    // when
    val preferenceList = PreferenceList.apply(initialPartitionId, n, ring)

    // then
    preferenceList shouldBe Left(PreferenceList.NotSufficientSize(PreferenceList(NodeId(1), List(NodeId(0)))))
  }
}
