package auction_system

import akka.actor.{Props, ActorSystem}
import auction_system.auction.Auction
import auction_system.auction_search.AuctionSearch
import auction_system.buyer.Buyer
import auction_system.seller.Seller

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionSystem extends App {
  val system = ActorSystem("Auction")
  val seller = system.actorOf(Props[Seller], "seller")
  val auctionSearch = system.actorOf(Props[AuctionSearch], "auction_search")
  val buyer1 = system.actorOf(Props[Buyer], "buyer1")
  val buyer2 = system.actorOf(Props[Buyer], "buyer2")
  val buyer3 = system.actorOf(Props[Buyer], "buyer3")
  val buyer4 = system.actorOf(Props[Buyer], "buyer4")

  seller ! Seller.Initialize(List("auction1", "auction2"))

  buyer1 ! Buyer.Initialize("auction1", 10)
  buyer2 ! Buyer.Initialize("auction1", 11)
  buyer3 ! Buyer.Initialize("auction1", 12)
  buyer4 ! Buyer.Initialize("auction2", 4)

  Await.result(system.whenTerminated, Duration.Inf)
}
