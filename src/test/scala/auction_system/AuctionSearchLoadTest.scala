package auction_system

import akka.actor.{ActorSystem, Props}
import akka.remote.ContainerFormats.ActorRef
import akka.routing.{RoundRobinRoutingLogic, ScatterGatherFirstCompletedRoutingLogic}
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import auction_system.auction_search.AuctionSearch.{Find, SearchResult}
import auction_system.master_search.MasterSearch
import auction_system.master_search.MasterSearch.{InitSearch, Search}
import auction_system.notifier.Notifier
import auction_system.notifier.Notifier.InitNotifier
import auction_system.publisher.Publisher
import auction_system.seller.{Active, Seller}
import auction_system.seller.Seller.Initialize
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random



class AuctionSearchLoadTest (_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                                                            with WordSpecLike
                                                                            with MustMatchers
                                                                            with BeforeAndAfterAll {

  def this() = this(ActorSystem("AuctionSearchTest"))

  override def afterAll: Unit = {
    val duration = stop - start
    print(s"$duration")
    system.terminate()
  }

  var start: Long = 0
  var stop: Long = 0
  val r = Random

  "A master search actor" must {
    val nOfSlaves = 4
    val logic = RoundRobinRoutingLogic()
//    val logic = ScatterGatherFirstCompletedRoutingLogic(1 second)

    val search = system.actorOf(Props[MasterSearch], "master_search")
    search ! InitSearch(nOfSlaves, logic)
    val notifier = TestProbe("notifier").ref
    val seller = TestFSMRef(new Seller, "seller")

    "register 50 000 auctions and search 10 000" in {
      val auctionNames: List[String] = List.tabulate(20000)(x => x.toString)
      start = System.currentTimeMillis()
      seller ! Initialize(auctionNames, notifier)
      awaitCond(seller.stateName == Active)

      for(x <- 1 to 50000) {
        search ! Search(r.nextInt(20000).toString)
        expectMsg(_: SearchResult)
      }
      stop = System.currentTimeMillis()
      assert(true)
    }

  }

}
