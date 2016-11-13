package auction_system.buyer

import akka.actor.{FSM, ActorRef}
import auction_system.auction_search.AuctionSearch
import scala.concurrent.duration._
import auction_system.auction.Auction

sealed trait BuyerState
case object Started extends BuyerState
case object FindAuction extends BuyerState
case object FirstBid extends BuyerState
case object Bidding extends BuyerState
case object Winning extends BuyerState
case object Passed extends BuyerState

sealed trait BuyerData
case object Uninitialized extends BuyerData
case class Searching(auctionName: String, maxOffer: Int) extends BuyerData {
  require(auctionName != null && maxOffer > 0)
}
case class Active(auction: ActorRef, var actualOffer: Int, maxOffer: Int) extends BuyerData {
  require(actualOffer >= 0 && maxOffer > 0 && actualOffer <= maxOffer && auction != null)
}
case class Passive(auction: ActorRef, actualOffer: Int, maxOffer: Int) extends BuyerData {
  require(actualOffer >= 0 && maxOffer > 0 && auction != null)
}

object Buyer {
  case class Initialize(auctionName: String, maxOffer: Int) {
    require(maxOffer > 0 && auctionName != null)
  }
}

class Buyer extends FSM[BuyerState, BuyerData] {
  import Auction._
  import Buyer._
  startWith(Started, Uninitialized)

  when(Started) {
    case Event(Initialize(auctionName, maxOffer), Uninitialized) =>
      println(s"${self.path.name} initialized")
      context.actorSelection("../auction_search") ! AuctionSearch.Find(auctionName)
      goto(FindAuction) using Searching(auctionName, maxOffer)
  }
  when(FindAuction, stateTimeout = 1 second) {
    case Event(AuctionSearch.SearchResult(auctions), t: Searching) =>
      val auction = auctions.head
      auction ! Bid(1)
      goto(Bidding) using Active(auction, 1, t.maxOffer)
    case Event(StateTimeout, t: Searching) =>
      context.actorSelection("../auction_search") ! AuctionSearch.Find(t.auctionName)
      stay using t
  }

  when(Bidding) {
    case Event(OfferAccepted, t: Active) =>
      println(s"${self.path.name} bids ${t.actualOffer}")
      goto(Winning) using t
    case Event(OfferRejected, t: Active) =>
      println(s"${self.path.name} got ${t.actualOffer} rejected")
      t.actualOffer = t.actualOffer + 1
      t.auction ! Bid(t.actualOffer)
      stay using t
    case Event(NewOffer(offer), t: Active) if offer > t.actualOffer =>
      t.actualOffer = t.actualOffer + 1
      t.auction ! Bid(t.actualOffer)
      stay using t
  }

  when(Winning) {
    case Event(NewOffer(amount), t: Active) if amount < t.maxOffer =>
      t.auction ! Bid(amount + 1)
      goto(Bidding) using new Active(t.auction, amount + 1, t.maxOffer)
    case Event(NewOffer(amount), t: Active) =>
      goto(Passed) using new Passive(t.auction, t.actualOffer, t.maxOffer)
    case Event(WinNotification, t: Active) =>
      println(s"${self.path.name} win ${t.auction.path.name}")
      stop
  }

  when(Passed) {
    case Event(NewOffer(offer), t: Passive) =>
      stay using t
    case Event(WinNotification, t: Passive) =>
      println(s"${self.path.name} win ${t.auction.path.name}")
      stop
    case Event(LooseNotification, t: Passive) =>
      println(s"${self.path.name} loose ${t.auction.path.name}")
      stop
  }

  initialize()
}
