package auction_system.seller

import akka.actor.{Props, ActorRef, FSM}
import auction_system.auction.Auction
import auction_system.auction.Auction.AuctionEnded
import auction_system.auction_search.AuctionSearch
import auction_system.master_search.MasterSearch
import scala.concurrent.Await
import scala.concurrent.duration._

sealed trait SellerState
case object Uninitialized extends SellerState
case object Active extends SellerState
case object WithoutAuctions extends SellerState

sealed trait SellerData
case object NoData extends SellerData
case class MyAuctions(auctions: List[ActorRef]) extends SellerData
case object NoAuctions extends SellerData

object Seller {
  case class Initialize(auctionNames: List[String], Notifier: ActorRef)
  case class NewAuction(auctionName: String)
}

class Seller extends FSM[SellerState, SellerData] {
  import Seller._
  startWith(Uninitialized, NoData)

  when(Uninitialized) {
    case Event(Initialize(auctionNames, notifier: ActorRef), NoData) =>
      println("Seller initialized")
      val auctions = for(name <- auctionNames) yield context.system.actorOf(Props[Auction], name)
      for(auction <- auctions) {
        context.actorSelection("../master_search") ! MasterSearch.Register(auction)
        auction ! Auction.Setup(self, notifier)
      }
      goto(Active) using MyAuctions(auctions)
  }

  when(Active) {
    case Event(AuctionEnded(auction), t: MyAuctions) =>
      t.auctions.filterNot(elem => elem == auction)
      if(t.auctions.nonEmpty)
        stay using t
      else
        goto(WithoutAuctions) using NoAuctions
    case Event(NewAuction(auctionName), t: MyAuctions) =>
      stay using new MyAuctions(context.actorOf(Props[Auction], auctionName) :: t.auctions)
  }

  when(WithoutAuctions, stateTimeout = 2 seconds) {
    case Event(NewAuction(name), NoAuctions) =>
      goto(Active) using MyAuctions(List(context.system.actorOf(Props[Auction], name)))
    case Event(StateTimeout, NoAuctions) =>
      stop
  }

  initialize()
}
