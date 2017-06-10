package justin.db.replica

import java.util.UUID

import justin.db.consistenthashing.{NodeId, Ring, UUID2RingPartitionId}
import justin.db.storage.PluggableStorageProtocol.DataOriginality

class IsPrimaryOrReplica(nodeId: NodeId, ring: Ring) extends (UUID => DataOriginality) {

  override def apply(id: UUID): DataOriginality = {
    val partitionId = UUID2RingPartitionId(id, ring)

    if(ring.getNodeId(partitionId).contains(nodeId)) {
      DataOriginality.Primary(partitionId)
    } else {
      DataOriginality.Replica(partitionId)
    }
  }
}
