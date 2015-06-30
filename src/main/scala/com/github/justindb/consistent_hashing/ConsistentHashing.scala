package com.github.justindb.consistent_hashing

import scala.collection.immutable.{ TreeMap, SortedMap }
import scala.util.hashing.MurmurHash3

trait Record[T] {
  def v: T
}
case class TextRecord(override val v: String) extends Record[String]
case class LongRecord(override val v: Long) extends Record[Long]

case class Node(id: Int, name: String, hash: Int = 0) {
  type RecordHash = Int
  type Key = (String, RecordHash)

  var store: Map[Key, Record[_]] = Map.empty

}

object Node {

  def addRecord[T](key: String, v: Record[T]): Option[Unit] = {
    for {
      recordHash <- Some(ConsistentHashing.hashFunc(key))
      id <- ConsistentHashing.getNodeId(recordHash)
      node <- ConsistentHashing.getNode(id)
    } yield {
      node.store = node.store + (((key, recordHash), v))
    }
  }

  def findRecord(key: String): Option[Record[_]] = {
    for {
      recordHash <- Some(ConsistentHashing.hashFunc(key))
      id <- ConsistentHashing.getNodeId(recordHash)
      node <- ConsistentHashing.getNode(id)
    } yield {
      node.store((key, recordHash))
    }
  }

}

object ConsistentHashing {

  var ring: SortedMap[Int, Node] = TreeMap.empty

  def hashFunc(key: String): Int = MurmurHash3.stringHash(key)

  def addNodes(realNodes: Int, virtualNodesPerNode: Int): Unit = {
    (1 to realNodes).foreach { i =>
      ConsistentHashing.add(Node(i, s"r=$i"), virtualNodesPerNode)
    }
  }

  private def add(node: Node, virtualNodesPerNode: Int): Unit = {
    (1 to virtualNodesPerNode).map { i =>
      ring = ring + ((hashFunc(node.name + i), node.copy(name = node.name + ":v=" + i.toString)))
    }
  }

  def getNode(id: Int): Option[Node] = {
    val node = ring(id)
    if (node == null)
      None
    else
      Some(node)
  }

  def getNodeId(entryKey: Any): Option[Int] = {
    if (ring.isEmpty)
      None
    else {
      val hash = hashFunc(entryKey.toString)
      val tailMap = ring.from(hash)

      val nodeKey = if (tailMap.isEmpty)
        ring.firstKey
      else
        tailMap.firstKey

      Some(nodeKey)
    }
  }
}