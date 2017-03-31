package justin.db.kryo

import com.esotericsoftware.kryo.Kryo
import com.typesafe.scalalogging.StrictLogging

class SerializerInit extends StrictLogging {

  def customize(kryo: Kryo): Unit = {
    logger.info("Initialized Kryo")

    // cluster
    kryo.register(classOf[justin.db.actors.protocol.RegisterNode], RegisterNodeSerializer, 50)

    // write -- request
    kryo.register(classOf[justin.db.actors.protocol.StorageNodeWriteDataLocal], StorageNodeWriteDataLocalSerializer, 51)

    // write -- responses
    kryo.register(classOf[justin.db.actors.protocol.StorageNodeFailedWrite],     StorageNodeWriteResponseSerializer, 52)
    kryo.register(classOf[justin.db.actors.protocol.StorageNodeSuccessfulWrite], StorageNodeWriteResponseSerializer, 53)
    kryo.register(classOf[justin.db.actors.protocol.StorageNodeConflictedWrite], StorageNodeWriteResponseSerializer, 54)

    // read - request
    kryo.register(classOf[justin.db.actors.protocol.StorageNodeLocalRead], StorageNodeLocalReadSerializer, 60)
  }
}
