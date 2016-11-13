package auction_system.publisher

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import auction_system.publisher.Publisher.Publish

object Publisher {
  case class Publish(auctionName: String, buyer: ActorRef, offer: Int)
}

class Publisher extends Actor{
  override def receive = LoggingReceive {
    case Publish(auctionName, buyer, offer) =>
      println(s"### Buyer ${buyer.path.name} bids $offer in auction $auctionName")
  }
}
