package auction_system.auction

import akka.actor.{ActorRef, FSM}
import scala.concurrent.duration._

sealed trait AuctionState
case object Created extends AuctionState
case object Activated extends AuctionState
case object Ignored extends AuctionState
case object Sold extends AuctionState

sealed trait AuctionData
case object NoData extends AuctionData
case class NoBids(seller: ActorRef) extends AuctionData {
  require(seller != null)
}
case class Bidding(buyers: List[ActorRef], winner: ActorRef, bestOffer: Int, seller: ActorRef) extends AuctionData {
  require(bestOffer > 0)
}
case class Finish(sellers: List[ActorRef], winner: ActorRef, bestOffer: Int, seller: ActorRef) extends AuctionData {
  require(bestOffer > 0)
}

object Auction {
  val createTime = 2 seconds
  val bidTime = 5 seconds
  val deleteTime = 1 second

  case class SetSeller(seller: ActorRef) {
    require(seller != null)
  }

  case class Bid(amount: Int) {
    require(amount > 0)
  }

  case class NewOffer(amount: Int) {
    require(amount > 0)
  }

  case object OfferAccepted
  case object OfferRejected
  case object WinNotification
  case object LooseNotification

  case class AuctionEnded(auction: ActorRef) {
    require(auction != null)
  }
}

class Auction extends FSM[AuctionState, AuctionData] {
  import Auction._
  startWith(Created, NoData)

  when(Created, stateTimeout = createTime) {
    case Event(SetSeller(seller), NoData) =>
      stay using NoBids(seller)
    case Event(Bid(amount), t: NoBids) =>
      println(s"${self.path.name} started")
      sender ! OfferAccepted
      goto(Activated) using Bidding(List(sender), sender, amount, t.seller)
    case Event(StateTimeout, t: NoBids) =>
      println(s"${self.path.name} ignored")
      goto(Ignored) using t
  }

  when(Activated, stateTimeout = bidTime) {
    case Event(Bid(amount), t: Bidding) if amount > t.bestOffer =>
      for(buyer <- t.buyers)
        if(buyer != sender)
          buyer ! NewOffer(amount)
      sender ! OfferAccepted
      if(!t.buyers.contains(sender))
        stay() using new Bidding(sender :: t.buyers, sender, amount, t.seller)
      else
        stay() using new Bidding(t.buyers, sender, amount, t.seller)
    case Event(Bid(amount), t: Bidding) =>
      sender ! OfferRejected
      if(!t.buyers.contains(sender))
        stay() using new Bidding(sender :: t.buyers, sender, amount, t.seller)
      else
        stay() using new Bidding(t.buyers, sender, amount, t.seller)
    case Event(StateTimeout, t: Bidding) =>
      println(s"${self.path.name} finished at ${t.bestOffer}")
      goto(Sold) using new Finish(t.buyers, t.winner, t.bestOffer, t.seller)
  }

  when(Ignored, stateTimeout = deleteTime) {
    case Event(StateTimeout, t: NoBids) =>
      println(s"${self.path.name} stopped")
      t.seller ! AuctionEnded(self)
      stop
  }

  when(Sold, stateTimeout = deleteTime) {
    case Event(StateTimeout, t: Finish) =>
      t.winner ! WinNotification
      for(buyer <- t.sellers)
        if(buyer != t.winner)
          buyer ! LooseNotification
      t.seller ! AuctionEnded(self)
      println(s"${self.path.name} stopped")
      stop
  }

  initialize()
}
