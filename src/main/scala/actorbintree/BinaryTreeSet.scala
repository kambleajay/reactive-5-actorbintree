/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.mutable
import akka.event.LoggingReceive

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
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue: mutable.Queue[Operation] = mutable.Queue.empty[Operation]

  // optional
  def receive = LoggingReceive {
    normal
  }

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = LoggingReceive {
    case Insert(caller, id, elem) => root ! Insert(caller, id, elem)
    case Contains(caller, id, elem) => root ! Contains(caller, id, elem)
    case Remove(caller, id, elem) => root ! Remove(caller, id, elem)
    case GC => {
      val newRoot = createRoot
      context.become(garbageCollecting(newRoot))
      root ! CopyTo(newRoot)
    }
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = LoggingReceive {
    case Insert(caller, id, elem) => pendingQueue.enqueue(Insert(caller, id, elem))
    case Contains(caller, id, elem) => pendingQueue.enqueue(Contains(caller, id, elem))
    case Remove(caller, id, elem) => pendingQueue.enqueue(Remove(caller, id, elem))
    case CopyFinished => {
      //println("tree copy finished!")

      while (!pendingQueue.isEmpty) {
        //println(pendingQueue)
        newRoot ! pendingQueue.dequeue
      }
      assert(pendingQueue.isEmpty)

      root ! PoisonPill
      root = newRoot
      context.become(normal)
    }
    case GC => ()
  }

}

