package justin.db

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import justin.consistent_hashing.{GetNodeIdByPartitionId, Ring}
import justin.db.StorageNodeActor.{GetValue, NodeRegistration, PutValue}
import justin.db.storage.PluggableStorage

case class StorageNodeActorId(id: Int) extends AnyVal

class StorageNodeActor(nodeId: StorageNodeActorId, storage: PluggableStorage, ring: Ring) extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  val nodeFromRing = new GetNodeIdByPartitionId(ring)

  override def receive: Receive = receive(Map.empty[StorageNodeActorId, ActorRef])

  private def receive(clusterMembers: Map[StorageNodeActorId, ActorRef]): Receive = {
    case GetValue(id)                 => sender() ! storage.get(id.toString)
    case pv @ PutValue(valueId, data) => handlePutValue(sender(), pv, clusterMembers)
    case state: CurrentClusterState   => state.members.filter(_.status == MemberStatus.Up).foreach(register)
    case NodeRegistration(senderNodeId) if !clusterMembers.contains(senderNodeId) =>
      context.become(receive(clusterMembers + (senderNodeId -> sender())))
      sender() ! NodeRegistration(nodeId)
    case MemberUp(m) => register(m)
    case t           => println("not handled msg: " + t)
  }

  private def handlePutValue(sender: ActorRef, pv: PutValue, clusterMembers: Map[StorageNodeActorId, ActorRef]) = {
    nodeFromRing(pv.id).foreach {
      case selectedNodeId if selectedNodeId.id == nodeId.id =>
        storage.put(pv.id.toString, pv.value)
        sender ! "ack"
      case selectedNodeId =>
        val storageNodeActorId = StorageNodeActorId(selectedNodeId.id)
        clusterMembers.get(storageNodeActorId).foreach(_ ! pv)
    }
  }

  private def register(member: Member) = {
    val name = if(nodeId.id == 1) "id-0" else "id-1" // TODO: should send to every logic NodeId
    context.actorSelection(RootActorPath(member.address) / "user" / s"$name") ! NodeRegistration(nodeId)
  }
}

object StorageNodeActor {

  sealed trait StorageNodeReq
  case class GetValue(id: UUID) extends StorageNodeReq
  case class PutValue(id: UUID, value: String) extends StorageNodeReq
  case class NodeRegistration(nodeId: StorageNodeActorId) extends StorageNodeReq

  def role: String = "StorageNode"

  def name(nodeId: StorageNodeActorId): String = s"id-${nodeId.id}"

  def props(nodeId: StorageNodeActorId, storage: PluggableStorage, ring: Ring): Props = {
    Props(new StorageNodeActor(nodeId, storage, ring))
  }
}
