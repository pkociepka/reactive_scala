package auction_system

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, TestFSMRef, ImplicitSender, TestKit}
import auction_system.auction.Auction
import auction_system.auction.Auction.{AuctionEnded, Setup}
import auction_system.auction_search.{MyAuctions, AuctionSearch}
import auction_system.notifier.Notifier
import auction_system.seller.Seller.Initialize
import auction_system.seller.{Active, Uninitialized, Seller}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}


class SellerTest (_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                                                 with WordSpecLike
                                                                 with MustMatchers
                                                                 with BeforeAndAfterAll {

  def this() = this(ActorSystem("AuctionSearchTest"))

  override def afterAll: Unit = {
    system.terminate()
  }

  "A Seller actor" must {
    val seller = TestFSMRef(new Seller)
    "start in Uninitialized state" in {
      assert(seller.stateName == Uninitialized)
    }

    val auctionSearcher = TestFSMRef(new AuctionSearch, "auction_search")
    val notifier = TestFSMRef(new Notifier, "notifier")
    "create and register an auction and switch to Active state" in {
      seller ! Initialize(List("a1"), notifier)
      assert(seller.stateName == Active)
      assert(auctionSearcher.stateData.asInstanceOf[MyAuctions].auctions.head.path.name == "a1")
    }
  }

  "An Auction actor" must {
    val seller = TestProbe()
    val notifier = TestFSMRef(new Notifier, "notifier")
    val auction = seller.childActorOf(Props[Auction])
    seller.send(auction, Setup(seller.ref, notifier))
    "report the end of an auction afret tieout" in {
      seller.expectMsg(AuctionEnded(auction))
    }
  }

}
