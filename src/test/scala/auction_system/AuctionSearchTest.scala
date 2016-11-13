package auction_system

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import auction_system.auction.Auction
import auction_system.auction_search.AuctionSearch
import auction_system.auction_search.AuctionSearch.NewAuction
import org.scalatest._

class AuctionSearchTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                                                       with WordSpecLike
                                                                       with MustMatchers
                                                                       with BeforeAndAfterAll {
  def this() = this(ActorSystem("AuctionSearchTest"))

  override def afterAll: Unit = {
    system.terminate()
  }

  "An AuctionSearch actor" must {
    val auctionSearcher = system.actorOf(Props[AuctionSearch], "auction_search")
    val auction = system.actorOf(Props[Auction], "auction1")
    auctionSearcher ! NewAuction(auction)

    "send back the auction ref" in {
      auctionSearcher ! AuctionSearch.Find("auction1")
      expectMsg(AuctionSearch.SearchResult(List(auction)))
    }
  }
}
