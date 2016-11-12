package auction_system.auction

import akka.actor.{ActorRef, FSM}
import scala.concurrent.duration._

sealed trait AuctionState
case object Created extends AuctionState
case object Activated extends AuctionState
case object Ignored extends AuctionState
case object Sold extends AuctionState

sealed trait AuctionData
case object NoBids extends AuctionData
case class Bidding(buyers: List[ActorRef], winner: ActorRef, bestOffer: Int) extends AuctionData {
  require(bestOffer > 0)
}
case class Finish(sellers: List[ActorRef], winner: ActorRef, bestOffer: Int) extends AuctionData {
  require(bestOffer > 0)
}

object Auction {
  val createTime = 2 seconds
  val bidTime = 5 seconds
  val deleteTime = 1 second

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
}

class Auction extends FSM[AuctionState, AuctionData] {
  import Auction._
  startWith(Created, NoBids)

  when(Created, stateTimeout = createTime) {
    case Event(Bid(amount), NoBids) =>
      println(s"${self.path.name} started")
      sender ! OfferAccepted
      goto(Activated) using Bidding(List(sender), sender, amount)
    case Event(StateTimeout, NoBids) =>
      println(s"${self.path.name} ignored")
      goto(Ignored) using NoBids
  }

  when(Activated, stateTimeout = bidTime) {
    case Event(Bid(amount), t: Bidding) if amount > t.bestOffer =>
      for(buyer <- t.buyers)
        if(buyer != sender)
          buyer ! NewOffer(amount)
      sender ! OfferAccepted
      if(!t.buyers.contains(sender))
        stay() using new Bidding(sender :: t.buyers, sender, amount)
      else
        stay() using new Bidding(t.buyers, sender, amount)
    case Event(Bid(amount), t: Bidding) =>
      sender ! OfferRejected
      if(!t.buyers.contains(sender))
        stay() using new Bidding(sender :: t.buyers, sender, amount)
      else
        stay() using new Bidding(t.buyers, sender, amount)
    case Event(StateTimeout, t: Bidding) =>
      println(s"${self.path.name} finished at ${t.bestOffer}")
      goto(Sold) using new Finish(t.buyers, t.winner, t.bestOffer)
  }

  when(Ignored, stateTimeout = deleteTime) {
    case Event(StateTimeout, NoBids) =>
      println(s"${self.path.name} stopped")
      stop
  }

  when(Sold, stateTimeout = deleteTime) {
    case Event(StateTimeout, t: Finish) =>
      t.winner ! WinNotification
      for(buyer <- t.sellers)
      if(buyer != t.winner)
      buyer ! LooseNotification
      println(s"${self.path.name} stopped")
      stop
  }

  initialize()
}
