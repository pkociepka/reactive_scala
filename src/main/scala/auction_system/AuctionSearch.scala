package auction_system.auction_search

import akka.actor.{ActorRef, FSM}
import auction_system.auction.Auction.AuctionEnded
import scala.concurrent.duration._

sealed trait AuctionSearchState
case object Active extends AuctionSearchState
case object WithoutAuctions extends AuctionSearchState

sealed trait AuctionSearchData
case class MyAuctions(auctions: List[ActorRef]) extends AuctionSearchData
case object NoAuctions extends AuctionSearchData

object AuctionSearch {
  case class NewAuction(auction: ActorRef)
  case class Find(auctionName: String)
  case class SearchResult(auctions: List[ActorRef])
}

class AuctionSearch extends FSM[AuctionSearchState, AuctionSearchData] {
  import AuctionSearch._
  startWith(WithoutAuctions, NoAuctions)

  when(Active) {
    case Event(AuctionEnded(auction), t: MyAuctions) =>
      t.auctions.filterNot(elem => elem == auction)
      if(t.auctions.nonEmpty)
        stay using t
      else
        goto(WithoutAuctions) using NoAuctions
    case Event(NewAuction(auction), t: MyAuctions) =>
      println(s"New auction ${auction.path.name} registered")
      stay using new MyAuctions(auction :: t.auctions)
    case Event(Find(name), t: MyAuctions) =>
      sender ! SearchResult(t.auctions.filter(elem => elem.path.name == name))
      stay using t
  }

  when(WithoutAuctions, stateTimeout = 2 seconds) {
    case Event(NewAuction(auction), NoAuctions) =>
      println(s"New auction ${auction.path.name} registered")
      goto(Active) using MyAuctions(List(auction))
    case Event(StateTimeout, NoAuctions) =>
      stop
  }

  initialize()
}
