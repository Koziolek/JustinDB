package justin.db

import akka.actor.{Actor, ActorRef, Props}
import justin.consistent_hashing.{NodeId, Ring}
import justin.db.StorageNodeActorProtocol._
import justin.db.replication.N
import justin.db.storage.PluggableStorageProtocol

class StorageNodeActor(nodeId: NodeId, storage: PluggableStorageProtocol, ring: Ring, n: N) extends Actor with ClusterSubscriberActor {

  private implicit val ec = context.dispatcher

  private val readCoordinator  = new ReplicaReadCoordinator(nodeId, ring, n, new ReplicaLocalReader(storage), new ReplicaRemoteReader)
  private val writeCoordinator = new ReplicaWriteCoordinator(nodeId, ring, n, new ReplicaLocalWriter(storage), new ReplicaRemoteWriter)

  private val coordinatorRouter = context.actorOf(
    props = RoundRobinCoordinatorRouter.props(readCoordinator, writeCoordinator),
    name  = RoundRobinCoordinatorRouter.routerName
  )

  def receive: Receive = receiveDataPF orElse receiveClusterDataPF(nodeId, ring) orElse notHandledPF

  private def receiveDataPF: Receive = {
    case readData: StorageNodeReadData   => coordinatorRouter ! ReplicaCoordinatorActorProtocol.ReadData(sender(), clusterMembers, readData)
    case writeData: StorageNodeWriteData => coordinatorRouter ! ReplicaCoordinatorActorProtocol.WriteData(sender(), clusterMembers, writeData)
  }

  private def notHandledPF: Receive = {
    case t => println("[StorageNodeActor] not handled msg: " + t)
  }
}

object StorageNodeActor {
  def role: String = "StorageNode"
  def name(nodeId: NodeId): String = s"id-${nodeId.id}"
  def props(nodeId: NodeId, storage: PluggableStorageProtocol, ring: Ring, n: N): Props = Props(new StorageNodeActor(nodeId, storage, ring, n))
}

case class StorageNodeActorRef(storageNodeActor: ActorRef) extends AnyVal
