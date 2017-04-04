package justin.db.actors

import akka.actor.{Actor, Address, RootActorPath, Terminated}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.{Cluster, MemberStatus}
import justin.consistent_hashing.{NodeId, Ring}
import justin.db.ClusterMembers
import justin.db.actors.protocol.RegisterNode

trait ClusterSubscriberActor { self: Actor =>

  private val cluster = Cluster(context.system)

  var clusterMembers: ClusterMembers = ClusterMembers.empty

  override def preStart(): Unit = cluster.subscribe(this.self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(this.self)

  def receiveClusterDataPF(nodeId: NodeId, ring: Ring): Receive = {
    case RegisterNode(senderNodeId) if clusterMembers.notContains(senderNodeId) =>
      context.watch(sender())
      clusterMembers = clusterMembers.add(senderNodeId, StorageNodeActorRef(sender()))
      sender() ! RegisterNode(nodeId)
    case MemberUp(m)                => register(nodeId, ring, m.address)
    case state: CurrentClusterState => state.members.filter(_.status == MemberStatus.Up).foreach(m => register(nodeId, ring, m.address))
    case Terminated(actorRef)       => clusterMembers = clusterMembers.removeByRef(StorageNodeActorRef(actorRef))
  }

  private def register(nodeId: NodeId, ring: Ring, address: Address) = {
    for {
      siblingNodeId <- ring.nodesId.filterNot(_ == nodeId)
      nodeName       = StorageNodeActor.name(siblingNodeId)
      nodeRef        = context.actorSelection(RootActorPath(address) / "user" / nodeName)
    } yield nodeRef ! RegisterNode(nodeId)
  }
}
