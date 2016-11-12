package auction_system.buyer

import akka.actor.{FSM, ActorRef}
import scala.concurrent.duration._
import auction_system.auction.Auction

sealed trait BuyerState
case object Started extends BuyerState
case object Loosing extends BuyerState
case object Bidding extends BuyerState
case object Winning extends BuyerState
case object Passed extends BuyerState

sealed trait BuyerData
case object Uninitialized extends BuyerData
case class Active(auction: ActorRef, actualOffer: Int, maxOffer: Int) extends BuyerData {
  require(actualOffer >= 0 && maxOffer > 0 && actualOffer <= maxOffer && auction != null)
}
case class Passive(auction: ActorRef, actualOffer: Int, maxOffer: Int) extends BuyerData {
  require(actualOffer >= 0 && maxOffer > 0 && actualOffer <= maxOffer && auction != null)
}

object Buyer {
  case class Initialize(auction: ActorRef, maxOffer: Int) {
    require(maxOffer > 0 && auction != null)
  }
}

class Buyer extends FSM[BuyerState, BuyerData] {
  import Auction._
  import Buyer._
  startWith(Started, Uninitialized)

  when(Started, stateTimeout = 10 microseconds) {
    case Event(Initialize(auction, maxOffer), Uninitialized) =>
      println(s"${self.path.name} initialized")
      auction ! Bid(1)
      stay using Passive(auction, 1, maxOffer)
    case Event(OfferAccepted, t: Passive) =>
      println(s"${self.path.name} bids ${t.actualOffer}")
      goto(Winning) using Active(t.auction, t.actualOffer, t.maxOffer)
    case Event(OfferRejected, t: Passive) =>
      println(s"${self.path.name} got ${t.actualOffer} rejected")
      t.auction ! Bid(t.actualOffer + 1)
      stay using Passive(t.auction, t.actualOffer + 1, t.maxOffer)
    case Event(StateTimeout, t: Passive) =>
      t.auction ! Bid(t.actualOffer)
      goto(Started) using t
  }

  when(Loosing) {
    case Event(NewOffer(amount), t: Active) if amount < t.maxOffer =>
      t.auction ! Bid(amount + 1)
      goto(Bidding) using new Active(t.auction, amount + 1, t.maxOffer)
    case Event(NewOffer(amount), t: Active) =>
      goto(Passed) using new Passive(t.auction, t.actualOffer, t.maxOffer)
    case Event(LooseNotification, t: Active) =>
      println(s"${self.path.name} loose")
      stop
  }

  when(Bidding) {
    case Event(OfferAccepted, t: Active) =>
      println(s"${self.path.name} bids ${t.actualOffer}")
      goto(Winning) using t
    case Event(OfferRejected, t: Active) =>
      println(s"${self.path.name} got ${t.actualOffer} rejected")
      goto(Loosing) using t
  }

  when(Winning) {
    case Event(NewOffer(amount), t: Active) if amount < t.maxOffer =>
      t.auction ! Bid(amount + 1)
      goto(Bidding) using new Active(t.auction, amount + 1, t.maxOffer)
    case Event(NewOffer(amount), t: Active) =>
      goto(Passed) using new Passive(t.auction, t.actualOffer, t.maxOffer)
    case Event(WinNotification, t: Active) =>
      println(s"${self.path.name} win")
      stop
  }

  when(Passed) {
    case Event(WinNotification, t: Passive) =>
      println(s"${self.path.name} win")
      stop
    case Event(LooseNotification, t: Passive) =>
      println(s"${self.path.name} loose")
      stop
  }

  initialize()
}
