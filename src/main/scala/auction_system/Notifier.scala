package auction_system.notifier

import akka.actor.{FSM, ActorRef}
import auction_system.publisher.Publisher.Publish

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
      t.publisher ! Publish(auctionName, buyer, offer)
      stay using t
  }

  initialize()
}
