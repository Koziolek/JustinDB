package justin.db.versioning

import java.nio.charset.StandardCharsets
import java.util.Base64

import justin.consistent_hashing.NodeId
import justin.db.versioning.DataVersioning.NodeIdVectorClock
import justin.vector_clocks.{Counter, VectorClock}
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.Try

class NodeIdVectorClockBase64 {

  def encode(vclock: NodeIdVectorClock): Try[String] = Try {
    val vcClockBytes = vclock.toList
      .map { case (nodeId, counter) => (nodeId.id.toString, counter.value) }
      .toJson
      .compactPrint
      .getBytes(StandardCharsets.UTF_8)

    Base64.getEncoder.encodeToString(vcClockBytes)
  }

  def decode(base64: String): Try[NodeIdVectorClock] = Try {
    val decodedMap = new String(Base64.getDecoder.decode(base64), StandardCharsets.UTF_8)
      .parseJson.convertTo[List[(String, Int)]]
      .map { case (k, v) => (NodeId(k.toInt), Counter(v))}
      .toMap

    VectorClock.apply(decodedMap)
  }
}
