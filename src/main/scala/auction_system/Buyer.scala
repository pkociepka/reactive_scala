package auction_system

import akka.actor.{FSM, ActorRef}

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

  when(Started) {
    case Event(Initialize(auction, maxOffer), Uninitialized) =>
      goto(Loosing) using new Active(auction, 0, maxOffer)
  }

  when(Loosing) {
    case Event(NewOffer(amount), t: Active) if amount < t.maxOffer =>
      t.auction ! Bid(amount + 1)
      goto(Bidding) using new Active(t.auction, amount + 1, t.maxOffer)
    case Event(NewOffer(amount), t: Active) =>
      goto(Passed) using new Passive(t.auction, t.actualOffer, t.maxOffer)
    case Event(LooseNotification, t: Active) =>
      stop
  }

  when(Bidding) {
    case Event(OfferAccepted, t: Active) =>
      goto(Winning) using t
    case Event(OfferRejected, t: Active) =>
      goto(Loosing) using t
  }

  when(Winning) {
    case Event(NewOffer(amount), t: Active) if amount < t.maxOffer =>
      t.auction ! Bid(amount + 1)
      goto(Bidding) using new Active(t.auction, amount + 1, t.maxOffer)
    case Event(NewOffer(amount), t: Active) =>
      goto(Passed) using new Passive(t.auction, t.actualOffer, t.maxOffer)
    case Event(WinNotification, t: Active) =>
      stop
  }

  when(Passed) {
    case Event(WinNotification, t: Passive) =>
      stop
    case Event(LooseNotification, t: Passive) =>
      stop
  }
}
