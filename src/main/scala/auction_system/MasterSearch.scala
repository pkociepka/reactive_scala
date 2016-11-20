package auction_system.master_search

import akka.actor.{ActorRef, Props, Actor}
import akka.routing.{RoundRobinRoutingLogic, BroadcastRoutingLogic, Router, ActorRefRoutee}
import auction_system.auction.Auction.AuctionEnded
import auction_system.auction_search.AuctionSearch
import auction_system.auction_search.AuctionSearch.{Find, NewAuction}
import auction_system.master_search.MasterSearch.{Ended, Search, Register}

object MasterSearch {
  case class Register(auction: ActorRef) {
    require(auction != null)
  }
  case class Search(auctionName: String) {
    require(auctionName != null)
  }
  case class Ended(auction: ActorRef) {
    require(auction != null)
  }
}

class MasterSearch extends Actor {
  val slaves = Vector.fill(5) {
    val s = context.actorOf(Props[AuctionSearch])
    context watch s
    ActorRefRoutee(s)
  }

  val registerRouter = Router(BroadcastRoutingLogic(), slaves)
  val searchRouter = Router(RoundRobinRoutingLogic(), slaves)

  override def receive: Receive = {
    case Register(auction) =>
      registerRouter.route(NewAuction(auction), sender)
    case Search(auctionName) =>
      searchRouter.route(Find(auctionName), sender)
    case Ended(auction) =>
      registerRouter.route(AuctionEnded(auction), sender)
  }
}
