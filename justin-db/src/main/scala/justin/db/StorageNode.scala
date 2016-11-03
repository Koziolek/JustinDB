package justin.db

import java.util.UUID

import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import justin.db.StorageNode.{GetValue, PutValue}
import justin.db.storage.PluggableStorage

case class StorageNodeId(id: Int) extends AnyVal

class StorageNode(nodeId: StorageNodeId, storage: PluggableStorage) extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case GetValue(id)        => sender() ! storage.get(id.toString)
    case PutValue(id, value) => storage.put(id.toString, value)
    case t                   => println("msg: " + t)
  }
}

object StorageNode {

  sealed trait StorageNodeReq
  case class GetValue(id: UUID) extends StorageNodeReq
  case class PutValue(id: UUID, value: String) extends StorageNodeReq

  def role: String = "StorageNode"

  def name(nodeId: StorageNodeId): String = s"id-${nodeId.id}"

  def props(nodeId: StorageNodeId, storage: PluggableStorage): Props = {
    Props(new StorageNode(nodeId, storage))
  }
}
