package auction_system

import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinRoutingLogic
import auction_system.auction_search.AuctionSearch
import auction_system.buyer.Buyer
import auction_system.master_search.MasterSearch
import auction_system.master_search.MasterSearch.InitSearch
import auction_system.notifier.Notifier
import auction_system.notifier.Notifier.InitNotifier
import auction_system.publisher.Publisher
import auction_system.seller.Seller
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionSystem extends App {
  val config = ConfigFactory.load()
  val auctionSystem = ActorSystem("Auction", config.getConfig("auctionapp").withFallback(config))
  val publisherSystem = ActorSystem("Publisher", config.getConfig("publisherapp").withFallback(config))
  val seller = auctionSystem.actorOf(Props[Seller], "seller")
  val masterSearch = auctionSystem.actorOf(Props[MasterSearch], "master_search")
  val buyer1 = auctionSystem.actorOf(Props[Buyer], "buyer1")
  val buyer2 = auctionSystem.actorOf(Props[Buyer], "buyer2")
  val buyer3 = auctionSystem.actorOf(Props[Buyer], "buyer3")
  val buyer4 = auctionSystem.actorOf(Props[Buyer], "buyer4")

  val notifier = auctionSystem.actorOf(Props[Notifier], "notifier")
  val publisher = publisherSystem.actorOf(Props[Publisher], "publisher")

  masterSearch ! InitSearch(5, RoundRobinRoutingLogic())

  notifier ! InitNotifier(publisher)

  seller ! Seller.Initialize(List("auction1", "auction2"), notifier)

  buyer1 ! Buyer.Initialize("auction1", 10)
  buyer2 ! Buyer.Initialize("auction1", 11)
  buyer3 ! Buyer.Initialize("auction1", 12)
  buyer4 ! Buyer.Initialize("auction2", 4)


  Await.result(auctionSystem.whenTerminated, Duration.Inf)
}
