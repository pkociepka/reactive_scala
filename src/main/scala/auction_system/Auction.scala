package auction_system

import akka.actor.{ActorRef, FSM}
import scala.concurrent.duration._

sealed trait AuctionState
case object Created extends AuctionState
case object Activated extends AuctionState
case object Ignored extends AuctionState
case object Sold extends AuctionState

sealed trait AuctionData
case object NoBids extends AuctionData
case class Bidding(sellers: List[ActorRef], winner: ActorRef, bestOffer: Int) extends AuctionData {
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
}

class Auction extends FSM[AuctionState, AuctionData] {
  import Auction._
  startWith(Created, NoBids)

  when(Created, stateTimeout = createTime) {
    case Event(Bid(amount), NoBids) =>
      goto(Activated) using Bidding(List(sender), sender, amount)
    case Event(StateTimeout, NoBids) =>
      goto(Ignored) using NoBids
  }

  when(Activated, stateTimeout = bidTime) {
    case Event(Bid(amount), t: Bidding) if amount > t.bestOffer =>
      if(!t.sellers.contains(sender))
        stay() using Bidding(sender :: t.sellers, sender, amount)
      else
        stay() using Bidding(t.sellers, sender, amount)
    case Event(StateTimeout, t: Bidding) =>
      goto(Sold) using Finish(t.sellers, t.winner, t.bestOffer)
  }

  when(Ignored, stateTimeout = deleteTime) {
    case Event(StateTimeout, NoBids) =>
      stop
  }

  when(Sold, stateTimeout = deleteTime) {
    case Event(StateTimeout, _) =>
      stop
  }
}
