/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.immutable.Queue
import actorbintree.BinaryTreeSet.{OperationFinished, Contains, ContainsResult, Insert}
import scala.Some

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef

    def id: Int

    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor with ActorLogging {

  import BinaryTreeSet._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root: Option[ActorRef] = None

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {

    case Insert(caller, id, elem) => {
      root match {
        case Some(rnode) => rnode ! Insert(caller, id, elem)
        case None => {
          root = Some(context.actorOf(BinaryTreeNode.props(elem, false)))
          caller ! OperationFinished(id)
        }
      }
    }

    case Contains(caller, id, elem) => {
      root match {
        case Some(rnode) => rnode ! Contains(caller, id, elem)
        case None => {
          caller ! ContainsResult(id, false)
        }
      }
    }

  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = ???

}

object BinaryTreeNode {

  trait Position

  case object Left extends Position

  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)

  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor with ActorLogging {


  //var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  var left: Option[ActorRef] = None
  var right: Option[ActorRef] = None

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {

    case Insert(caller, id, newElem) => {
      if (newElem < elem) {
        insertInLeft(caller, id, newElem)
      } else if (newElem > elem) {
        insertInRight(caller, id, newElem)
      } else {
        caller ! OperationFinished(id)
      }
    }

    case Contains(caller, id, elemToMatch) => {
      if (elemToMatch == elem) {
        caller ! ContainsResult(id, true)
      } else if (elemToMatch < elem) {
        containsInLeft(caller, id, elemToMatch)
      } else {
        containsInRight(caller, id, elemToMatch)
      }
    }

  }

  def insertInLeft(caller: ActorRef, id: Int, elem: Int) {
    left match {
      case Some(lnode) => lnode ! Insert(caller, id, elem)
      case None => {
        left = Some(context.actorOf(BinaryTreeNode.props(elem, false)))
        caller ! OperationFinished(id)
      }
    }
  }

  def insertInRight(caller: ActorRef, id: Int, elem: Int) {
    right match {
      case Some(rnode) => rnode ! Insert(caller, id, elem)
      case None => {
        right = Some(context.actorOf(BinaryTreeNode.props(elem, false)))
        caller ! OperationFinished(id)
      }
    }
  }

  def containsInLeft(caller: ActorRef, id: Int, elemToMatch: Int) {
    left match {
      case Some(lnode) => lnode ! Contains(caller, id, elem)
      case None => {
        caller ! ContainsResult(id, false)
      }
    }
  }

  def containsInRight(caller: ActorRef, id: Int, elemToMatch: Int) {
    right match {
      case Some(rnode) => rnode ! Contains(caller, id, elem)
      case None => {
        caller ! ContainsResult(id, false)
      }
    }
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = ???

}
