package auction_system

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestFSMRef, ImplicitSender, TestKit}
import auction_system.auction.Auction.Setup
import auction_system.auction._
import auction_system.seller.Seller
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

class AuctionTest (_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                                                  with WordSpecLike
                                                                  with MustMatchers
                                                                  with BeforeAndAfterAll {

  def this() = this(ActorSystem("AuctionSearchTest"))

  override def afterAll: Unit = {
    system.terminate()
  }

//  "An Auction actor" must {
//    val auction = TestFSMRef(new Auction)
//    val seller = system.actorOf(Props[Seller], "seller")
//    "start in Created state" in {
//      assert(auction.stateName == Created)
//    }
//    "set a seller properly" in {
//      auction ! SetSeller(seller)
//      assert(auction.stateData.asInstanceOf[NoBids].seller == seller)
//    }
//    "change state to Activated on the first bid" in {
//      auction ! Auction.Bid(1)
//      assert(auction.stateName == Activated)
//    }
//    "remember last bid and winner" in {
//      assert(auction.stateData.asInstanceOf[Bidding].bestOffer == 1)
//      assert(auction.stateData.asInstanceOf[Bidding].winner == self)
//      assert(auction.stateData.asInstanceOf[Bidding].buyers == List(self))
//    }
//  }
}
