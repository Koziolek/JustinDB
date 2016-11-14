package justin.db

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import justin.db.consistent_hashing.{NodeId, Ring, UUID2RingPartitionId}
import justin.db.StorageNodeActor.{GetValue, PutReplicatedValue, PutValue, RegisterNode}
import justin.db.replication.{N, PreferenceList, W}
import justin.db.storage.PluggableStorage

case class StorageNodeActorRef(storageNodeActor: ActorRef) extends AnyVal

class StorageNodeActor(nodeId: NodeId, storage: PluggableStorage, ring: Ring, replication: N) extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = receive(ClusterMembers.empty)

  private def receive(clusterMembers: ClusterMembers): Receive = {
    case GetValue(id)                      => sender() ! storage.get(id.toString)
    case pv: PutValue                      => handlePutValue(pv, clusterMembers); sender() ! "ack"
    case PutReplicatedValue(valueId, data) => saveValue(valueId, data); sender() ! WriteNodeActor.SuccessfulWrite
    case state: CurrentClusterState        => state.members.filter(_.status == MemberStatus.Up).foreach(register)
    case RegisterNode(senderNodeId) if clusterMembers.notContains(senderNodeId) =>
      val members = clusterMembers.add(senderNodeId, StorageNodeActorRef(sender()))
      context.become(receive(members))
      sender() ! RegisterNode(nodeId)
    case MemberUp(m)                       => register(m)
    case t                                 => println("not handled msg: " + t)
  }

  private def saveValue(id: UUID, value: String) = storage.put(id.toString, value)

  private def handlePutValue(pv: PutValue, clusterMembers: ClusterMembers) = {
    val basePartitionId = new UUID2RingPartitionId(ring).apply(pv.id)
    val preferenceList  = PreferenceList(basePartitionId, replication, ring)

    preferenceList.foreach {
      case nId if nId == nodeId => saveValue(pv.id, pv.value)
      case nId => clusterMembers.get(nId).foreach(_.storageNodeActor ! PutReplicatedValue(pv.id, pv.value))
    }
  }

  private def register(member: Member) = {
    val name = if(nodeId.id == 1) "id-0" else "id-1" // TODO: should send to every logic NodeId
    context.actorSelection(RootActorPath(member.address) / "user" / s"$name") ! RegisterNode(nodeId)
  }
}

object StorageNodeActor {

  sealed trait StorageNodeMsg
  case class GetValue(id: UUID) extends StorageNodeMsg
  case class PutValue(w: W, id: UUID, value: String) extends StorageNodeMsg
  case class PutReplicatedValue(id: UUID, value: String) extends StorageNodeMsg
  case class RegisterNode(nodeId: NodeId) extends StorageNodeMsg
  case object SuccessfulWrite extends StorageNodeMsg
  case object FailedWrite extends StorageNodeMsg

  def role: String = "StorageNode"

  def name(nodeId: NodeId): String = s"id-${nodeId.id}"

  def props(nodeId: NodeId, storage: PluggableStorage, ring: Ring, replication: N): Props = {
    Props(new StorageNodeActor(nodeId, storage, ring, replication))
  }
}
