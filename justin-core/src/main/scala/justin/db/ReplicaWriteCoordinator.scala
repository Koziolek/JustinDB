package justin.db

import justin.consistent_hashing.{NodeId, Ring, UUID2RingPartitionId}
import justin.db.StorageNodeActorProtocol._
import justin.db.replication.{N, PreferenceList, W}

import scala.concurrent.{ExecutionContext, Future}

class ReplicaWriteCoordinator(
  nodeId: NodeId, ring: Ring, n: N,
  localDataWriter: ReplicaLocalWriter,
  remoteDataWriter: ReplicaRemoteWriter
)(implicit ec: ExecutionContext) extends ((StorageNodeWriteData, ClusterMembers) => Future[StorageNodeWritingResult]) {

  override def apply(cmd: StorageNodeWriteData, clusterMembers: ClusterMembers): Future[StorageNodeWritingResult] = cmd match {
    case StorageNodeWriteData.Local(data)        => coordinateLocal(data)
    case StorageNodeWriteData.Replicate(w, data) => coordinateReplicated(w, data, clusterMembers)
  }

  private def coordinateLocal(data: Data) = localDataWriter.apply(data, new ResolveDataOriginality(nodeId, ring))

  private def coordinateReplicated(w: W, data: Data, clusterMembers: ClusterMembers) = {
    val ringPartitionId = UUID2RingPartitionId.apply(data.id, ring)

    PreferenceList(ringPartitionId, n, ring) match {
      case Left(PreferenceList.LackOfCoordinator)                 => Future.successful(StorageNodeWritingResult.FailedWrite)
      case Left(PreferenceList.NotSufficientSize(preferenceList)) => Future.successful(StorageNodeWritingResult.FailedWrite)
      case Right(preferenceList) =>
        val updatedData = Data.updateVclock(data, preferenceList)
        ResolveNodeTargets(nodeId, preferenceList, clusterMembers) match {
          case ResolvedTargets(true, remotes)  if remotes.size + 1 >= w.w =>
            (coordinateLocal(updatedData) zip remoteDataWriter.apply(remotes, updatedData)).map(converge).map(ReachConsensusReplicatedWrites(w))
          case ResolvedTargets(false, remotes) if remotes.size     >= w.w =>
            remoteDataWriter.apply(remotes, updatedData).map(ReachConsensusReplicatedWrites(w))
          case _ => Future.successful(StorageNodeWritingResult.FailedWrite)
        }
    }
  }
}
