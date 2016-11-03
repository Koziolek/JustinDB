package justin.db

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import justin.db.storage.PluggableStorage

case class StorageNodeId(id: Int) extends AnyVal

class StorageNode(nodeId: StorageNodeId, storage: PluggableStorage) extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = ???
}

object StorageNode {
  def buildPreferenceList(baseId: StorageNodeId, replicationFactor: ReplicationFactor): List[StorageNodeId] = {
    val floor   = baseId.id + 1
    val ceiling = baseId.id + replicationFactor.n

    (floor to ceiling)
      .filterNot(_ == baseId.id)
      .map(StorageNodeId)
      .toList
  }
}

case class ReplicationFactor(n: Int) extends AnyVal
