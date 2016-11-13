package auction_system

import akka.actor.{Props, ActorSystem}
import auction_system.auction_search.AuctionSearch
import auction_system.buyer.Buyer
import auction_system.seller.Seller
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionSystem extends App {
  val config = ConfigFactory.load()
  val auctionSystem = ActorSystem("Auction", config.getConfig("auctionapp").withFallback(config))
  val seller = auctionSystem.actorOf(Props[Seller], "seller")
  val auctionSearch = auctionSystem.actorOf(Props[AuctionSearch], "auction_search")
  val buyer1 = auctionSystem.actorOf(Props[Buyer], "buyer1")
  val buyer2 = auctionSystem.actorOf(Props[Buyer], "buyer2")
  val buyer3 = auctionSystem.actorOf(Props[Buyer], "buyer3")
  val buyer4 = auctionSystem.actorOf(Props[Buyer], "buyer4")

  seller ! Seller.Initialize(List("auction1", "auction2"))

  buyer1 ! Buyer.Initialize("auction1", 10)
  buyer2 ! Buyer.Initialize("auction1", 11)
  buyer3 ! Buyer.Initialize("auction1", 12)
  buyer4 ! Buyer.Initialize("auction2", 4)

  Await.result(auctionSystem.whenTerminated, Duration.Inf)
}
