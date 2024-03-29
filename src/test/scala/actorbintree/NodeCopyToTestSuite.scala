/**
* Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
*/
package actorbintree

import akka.actor.{ Props, ActorRef, ActorSystem }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import org.scalatest.matchers.ShouldMatchers
import scala.util.Random
import scala.concurrent.duration._
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeCopyToTestSuite(_system: ActorSystem) extends TestKit(_system) with FunSuite with ShouldMatchers with BeforeAndAfterAll with ImplicitSender
{
  def this() = this(ActorSystem("PostponeSpec"))

  override def afterAll: Unit = system.shutdown()

  import actorbintree.BinaryTreeSet._

  test("empty leaf node confirms copy without actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, false))
  }

  test("active leaf node confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = false))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, true))
  }

  test("empty node with two children confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! Insert(testActor, 1, 1)
    expectMsg(OperationFinished(1))
    nodeUnderTest ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))

    nodeUnderTest ! Insert(testActor, -1, -1)
    expectMsg(OperationFinished(-1))
    nodeUnderTest ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, false))

    newNode ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))

    newNode ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))
  }

  test("active node with two children confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = false))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! Insert(testActor, 1, 1)
    expectMsg(OperationFinished(1))
    nodeUnderTest ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))

    nodeUnderTest ! Insert(testActor, -1, -1)
    expectMsg(OperationFinished(-1))
    nodeUnderTest ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, true))

    newNode ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))

    newNode ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))
  }

  test("empty node with left child confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! Insert(testActor, -1, -1)
    expectMsg(OperationFinished(-1))
    nodeUnderTest ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, false))

    newNode ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))
  }

  test("active node with left child confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = false))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! Insert(testActor, -1, -1)
    expectMsg(OperationFinished(-1))
    nodeUnderTest ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, true))

    newNode ! Contains(testActor, id = -1, -1)
    expectMsg(ContainsResult(-1, true))
  }

  test("empty node with right child confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! Insert(testActor, 1, 1)
    expectMsg(OperationFinished(1))
    nodeUnderTest ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, false))

    newNode ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))
  }

  test("active node with right child confirms copy while actually copying to new node") {
    val nodeUnderTest = system.actorOf(BinaryTreeNode.props(0, initiallyRemoved = false))
    val newNode = system.actorOf(BinaryTreeNode.props(1, initiallyRemoved = true))

    nodeUnderTest ! Insert(testActor, 1, 1)
    expectMsg(OperationFinished(1))
    nodeUnderTest ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))

    nodeUnderTest ! BinaryTreeNode.CopyTo(newNode)
    expectMsg(BinaryTreeNode.CopyFinished)

    newNode ! Contains(testActor, id = 0, 0)
    expectMsg(ContainsResult(0, true))

    newNode ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, true))
  }
}