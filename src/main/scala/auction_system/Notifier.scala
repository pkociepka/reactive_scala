package auction_system.notifier

import java.net.{UnknownHostException, PortUnreachableException, ConnectException}

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, FSM, ActorRef}
import akka.event.LoggingReceive
import auction_system.publisher.Publisher.Publish

import scala.util.Random

sealed trait NotifierState
case object UninitializedNotifier extends NotifierState
case object ActiveNotifier extends NotifierState

sealed trait NotifierData
case object NoData extends NotifierData
case class WithPublisher(publisher: ActorRef) extends NotifierData

object Notifier {
  case class InitNotifier(publisher: ActorRef)
  case class Notify(auctionName: String, buyer: ActorRef, offer: Int)
}

class Notifier extends FSM[NotifierState, NotifierData]{
  import Notifier._
  startWith(UninitializedNotifier, NoData)

  when(UninitializedNotifier) {
    case Event(InitNotifier(publisher), NoData) =>
      goto(ActiveNotifier) using WithPublisher(publisher)
  }

  when(ActiveNotifier) {
    case Event(Notify(auctionName, buyer, offer), t: WithPublisher) =>
//      t.publisher ! Publish(auctionName, buyer, offer)
      context.system.actorOf(Props[NotifierRequest]) ! NotifierRequest.Request(auctionName, buyer, offer, t.publisher)
      stay using t
  }

  initialize()
}

object NotifierRequest {
  case class Request(auctionName: String, buyer: ActorRef, offer: Int, publisher: ActorRef)
}

class NotifierRequest extends Actor {
  import NotifierRequest._
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  val rnd = Random

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ConnectException => Resume
      case _: PortUnreachableException => Stop
      case _: UnknownHostException => Escalate
    }

  override def receive = LoggingReceive {
    case Request(auctionName, buyer, offer, publisher) =>
      if(rnd.nextFloat() < 0.1)
        throw new PortUnreachableException
      publisher ! Publish(auctionName, buyer, offer)
  }
}