package justin.merkle_trees

import org.scalatest.{FlatSpec, Matchers}

class MerkleTreeTest extends FlatSpec with Matchers {

  behavior of "Merkle Tree"

  it should "have the same top hash" in {
    val data: Seq[Block] = Seq(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](7,8,9),
      Array[Byte](10,11,12)
    )
    val data2: Seq[Block] = Seq(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](7,8,9),
      Array[Byte](10,11,12)
    )

    val digest1 = MerkleTree.unapply(data)(MerkleDigest.CRC32).get.digest
    val digest2 = MerkleTree.unapply(data2)(MerkleDigest.CRC32).get.digest

    digest1.hash.deep shouldBe digest2.hash.deep
  }

  it should "have a different top hash" in {
    val data: Seq[Block] = Seq(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](7,8,9),
      Array[Byte](10,11,12)
    )
    val data2: Seq[Block] = Seq(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](9,8,7),
      Array[Byte](12,11,10)
    )

    val digest1 = MerkleTree.unapply(data)(MerkleDigest.CRC32).get.digest
    val digest2 = MerkleTree.unapply(data2)(MerkleDigest.CRC32).get.digest

    digest1.hash.deep should not be digest2.hash.deep
  }

  it should "use an all-zeros value to complete the pair" in {
    val oddDataSet: Seq[Block] = Seq(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](7,8,9)
    )

    val sameWithZeroed: Seq[Block] = Seq(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](7,8,9),
      Array[Byte](0)
    )

    val digest1 = MerkleTree.unapply(oddDataSet)(MerkleDigest.CRC32).get.digest
    val digest2 = MerkleTree.unapply(sameWithZeroed)(MerkleDigest.CRC32).get.digest

    digest1.hash.deep shouldBe digest2.hash.deep
  }

  it should "create missed zeroed byte blocks if initial blocks size is not multiplication of 2" in {
    val init = Array(
      Array[Byte](1,2,3),
      Array[Byte](4,5,6),
      Array[Byte](7,8,9),
      Array[Byte](7,8,9),
      Array[Byte](7,8,9)
    )

    val expected =  Array(
      Array[Byte](0),
      Array[Byte](0),
      Array[Byte](0)
    )

    val zeroed = MerkleTree.zeroed(init)

    zeroed.deep shouldBe expected.deep
  }
}
