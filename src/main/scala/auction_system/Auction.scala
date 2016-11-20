package auction_system.auction

import akka.actor.ActorRef
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import auction_system.master_search.MasterSearch
import auction_system.notifier.Notifier
import scala.concurrent.duration._
import scala.reflect._

sealed trait AuctionState extends FSMState
case object Created extends AuctionState {
  override def identifier: String = "created"
}
case object Activated extends AuctionState {
  override def identifier: String = "activated"
}
case object Ignored extends AuctionState {
  override def identifier: String = "ignored"
}
case object Sold extends AuctionState {
  override def identifier: String = "sold"
}

sealed trait AuctionData
case object NoData extends AuctionData
case class NoBids(seller: ActorRef, notifier: ActorRef) extends AuctionData {
  require(seller != null)
}
case class Bidding(buyers: List[ActorRef], winner: ActorRef, bestOffer: Int, seller: ActorRef, notifier: ActorRef) extends AuctionData {
  require(bestOffer > 0)
}
case class Finish(sellers: List[ActorRef], winner: ActorRef, bestOffer: Int, seller: ActorRef, notifier: ActorRef) extends AuctionData {
  require(bestOffer > 0)
}

sealed trait AuctionEvent
case class SetupEvent(seller: ActorRef, notifier: ActorRef) extends AuctionEvent
case class BidEvent(bid: Int, buyer: ActorRef) extends AuctionEvent
case object AuctionIgnoredEvent extends AuctionEvent
case object SoldEvent extends AuctionEvent
case object AuctionStoppedEvent extends AuctionEvent

object Auction {
  val createTime = 2 seconds
  val bidTime = 5 seconds
  val deleteTime = 1 second

  case class Setup(seller: ActorRef, notifier: ActorRef) {
    require(seller != null, notifier != null)
  }

  case class Bid(amount: Int) {
    require(amount > 0)
  }

  case class NewOffer(amount: Int) {
    require(amount > 0)
  }

  case object OfferAccepted
  case object OfferRejected
  case object WinNotification
  case object LooseNotification

  case class AuctionEnded(auction: ActorRef) {
    require(auction != null)
  }
}

class Auction extends PersistentFSM[AuctionState, AuctionData, AuctionEvent] {

  override def persistenceId: String = "persistent_auction"
  override def domainEventClassTag: ClassTag[AuctionEvent] = classTag[AuctionEvent]

  import Auction._

  startWith(Created, NoData)

  when(Created, stateTimeout = createTime) {
    case Event(Setup(seller, notifier), NoData) =>
      stay applying SetupEvent(seller, notifier)
    case Event(Bid(amount), t: NoBids) =>
      println(s"${self.path.name} started")
      sender ! OfferAccepted
      notify(t.notifier, sender, amount)
      goto(Activated) applying BidEvent(amount, sender)
    case Event(StateTimeout, t: NoBids) =>
      println(s"${self.path.name} ignored")
      goto(Ignored) applying AuctionIgnoredEvent
  }

  when(Activated, stateTimeout = bidTime) {
    case Event(Bid(amount), t: Bidding) if amount > t.bestOffer =>
      for (buyer <- t.buyers)
        if (buyer != sender)
          buyer ! NewOffer(amount)
      sender ! OfferAccepted
      notify(t.notifier, sender, amount)
      stay() applying BidEvent(amount, sender)
    case Event(Bid(amount), t: Bidding) =>
      sender ! OfferRejected
      stay() applying BidEvent(amount, sender)
    case Event(StateTimeout, t: Bidding) =>
      println(s"${self.path.name} finished at ${t.bestOffer}")
      goto(Sold) applying SoldEvent
  }

  when(Ignored, stateTimeout = deleteTime) {
    case Event(StateTimeout, t: NoBids) =>
      println(s"${self.path.name} stopped")
      t.seller ! AuctionEnded(self)
      context.actorSelection("../auction_search") ! AuctionEnded(self)
      stop applying AuctionStoppedEvent
  }

  when(Sold, stateTimeout = deleteTime) {
    case Event(StateTimeout, t: Finish) =>
      t.winner ! WinNotification
      for (buyer <- t.sellers)
        if (buyer != t.winner)
          buyer ! LooseNotification
      t.seller ! AuctionEnded(self)
      context.actorSelection("../master_search") ! MasterSearch.Ended(self)
      println(s"${self.path.name} stopped")
      stop applying AuctionStoppedEvent
  }

  override def applyEvent(event: AuctionEvent, dataBeforeEvent: AuctionData): AuctionData = {
    event match {
      case SetupEvent(seller, notifier) => NoBids(seller, notifier)

      case BidEvent(offer, buyer) =>
        dataBeforeEvent match {
          case data: NoBids => Bidding(List(buyer), buyer, offer, data.seller, data.notifier)
          case data: Bidding =>
            if (data.buyers.contains(buyer))
              Bidding(data.buyers, sender, offer, data.seller, data.notifier)
            else
              Bidding(sender :: data.buyers, sender, offer, data.seller, data.notifier)
        }

      case AuctionIgnoredEvent =>
        dataBeforeEvent

      case SoldEvent =>
        val data = dataBeforeEvent.asInstanceOf[Bidding]
        Finish(data.buyers, data.winner, data.bestOffer, data.seller, data.notifier)

      case AuctionStoppedEvent =>
        dataBeforeEvent
    }
  }

  def notify(notifier: ActorRef, buyer: ActorRef, offer: Int): Unit = {
    notifier.tell(Notifier.Notify(self.path.name, buyer, offer), self)
  }

  initialize()
}

