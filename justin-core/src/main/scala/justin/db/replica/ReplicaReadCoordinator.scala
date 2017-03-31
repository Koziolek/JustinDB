package justin.db.replica

import java.util.UUID

import justin.consistent_hashing.{NodeId, Ring, UUID2RingPartitionId}
import justin.db._
import justin.db.replica.ReplicaReadAgreement.ReadAgreement
import justin.db.actors.protocol.{StorageNodeLocalRead, StorageNodeReadRequest, StorageNodeReadResponse}

import scala.concurrent.{ExecutionContext, Future}

class ReplicaReadCoordinator(
  nodeId: NodeId, ring: Ring, n: N,
  localDataReader: ReplicaLocalReader,
  remoteDataReader: ReplicaRemoteReader
)(implicit ec: ExecutionContext) extends ((StorageNodeReadRequest, ClusterMembers) => Future[StorageNodeReadResponse]) {

  override def apply(cmd: StorageNodeReadRequest, clusterMembers: ClusterMembers): Future[StorageNodeReadResponse] = cmd match {
    case StorageNodeLocalRead(id)         => readLocalData(id)
    case StorageNodeReadRequest.Replicated(r, id) => coordinateReplicated(r, id, clusterMembers)
  }

  private def readLocalData(id: UUID) = localDataReader.apply(id, new IsPrimaryOrReplica(nodeId, ring))

  private def coordinateReplicated(r: R, id: UUID, clusterMembers: ClusterMembers) = {
    val partitionId = UUID2RingPartitionId.apply(id, ring)
    PreferenceList(partitionId, n, ring).fold(onLeft, onRight(r, id, clusterMembers))
  }

  private def onLeft(err: PreferenceList.Error) = Future.successful(StorageNodeReadResponse.FailedRead)

  private def onRight(r: R, id: UUID, clusterMembers: ClusterMembers)(preferenceList: PreferenceList) = {
    val consensusFuture = gatherReads(r, id, clusterMembers, preferenceList).map(new ReplicaReadAgreement().reach(r))
    consensusFuture.foreach(triggerReadRepairIfConsequent)
    consensusFuture.map(consensus2ReadingResult)
  }

  private def gatherReads(r: R, id: UUID, clusterMembers: ClusterMembers, preferenceList: PreferenceList) = {
    ResolveNodeAddresses(nodeId, preferenceList, clusterMembers) match {
      case ResolvedNodeAddresses(true, remotes)  if remotes.size + 1 >= r.r => (readLocalData(id) zip remoteDataReader.apply(remotes, id)).map(converge)
      case ResolvedNodeAddresses(false, remotes) if remotes.size >= r.r     => remoteDataReader.apply(remotes, id)
      case _                                                                => Future.successful(List(StorageNodeReadResponse.FailedRead))
    }
  }

  private def triggerReadRepairIfConsequent: PartialFunction[ReadAgreement, Unit] = {
    case ReadAgreement.Consequent(data) => println(s"Read Repair is fired of for $data") // TODO: finish this part with real logic
  }

  private def consensus2ReadingResult: ReadAgreement => StorageNodeReadResponse = {
    case ReadAgreement.Consequent(data) => StorageNodeReadResponse.StorageNodeFoundRead(data)
    case ReadAgreement.Found(data)      => StorageNodeReadResponse.StorageNodeFoundRead(data)
    case ReadAgreement.Conflicts(data)  => StorageNodeReadResponse.Conflicts(data)
    case ReadAgreement.NotEnoughFound   => StorageNodeReadResponse.NotFound
    case ReadAgreement.AllFailed        => StorageNodeReadResponse.FailedRead
    case ReadAgreement.AllNotFound      => StorageNodeReadResponse.NotFound
  }
}
