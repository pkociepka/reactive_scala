package auction_system

import akka.actor.{Props, ActorSystem}
import auction_system.auction.Auction
import auction_system.buyer.Buyer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionSystem extends App {
  val system = ActorSystem("Auction")
  val auction1 = system.actorOf(Props[Auction], "auction1")
  val auction2 = system.actorOf(Props[Auction], "auction2")
  val buyer1 = system.actorOf(Props[Buyer], "buyer1")
  val buyer2 = system.actorOf(Props[Buyer], "buyer2")
  val buyer3 = system.actorOf(Props[Buyer], "buyer3")
  val buyer4 = system.actorOf(Props[Buyer], "buyer4")

  buyer1 ! Buyer.Initialize(auction1, 10)
  buyer2 ! Buyer.Initialize(auction1, 11)
  buyer3 ! Buyer.Initialize(auction1, 12)
  buyer4 ! Buyer.Initialize(auction2, 4)

  Await.result(system.whenTerminated, Duration.Inf)
}
