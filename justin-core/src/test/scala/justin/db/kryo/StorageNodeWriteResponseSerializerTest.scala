package justin.db.kryo

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import justin.db.actors.protocol.StorageNodeSuccessfulWrite
import org.scalatest.{FlatSpec, Matchers}

class StorageNodeWriteResponseSerializerTest extends FlatSpec with Matchers {

  behavior of "StorageNodeWriteResponseSerializer Serializer"

  it should "serialize/deserialize StorageNodeSuccessfulWrite" in {
    // kryo init
    val kryo = new Kryo()
    kryo.register(classOf[StorageNodeSuccessfulWrite], StorageNodeWriteResponseSerializer)

    // object
    val serializedData = StorageNodeSuccessfulWrite(UUID.randomUUID())

    // serialization
    val bos    = new ByteArrayOutputStream()
    val output = new Output(bos)
    val bytes  = kryo.writeObject(output, serializedData)
    output.flush()

    // deserialization
    val bis              = new ByteArrayInputStream(bos.toByteArray)
    val input            = new Input(bis)
    val deserializedData = kryo.readObject(input, classOf[StorageNodeSuccessfulWrite])

    serializedData shouldBe deserializedData
  }

}
