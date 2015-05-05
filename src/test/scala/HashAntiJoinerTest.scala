import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Created by Maginecz on 4/19/2015.
 */
class HashAntiJoinerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "AntiHasJoin" must {
    "retain positive nodes" in {
      val primaryVec = Vector(15, 16, 17, 18)
      val prim = ChangeSet(
        positive = Vector(primaryVec)
      )
      val sec = ChangeSet(
        positive = Vector(Vector(13, 15, 16))
      )
      val actor = TestActorRef(new HashAntiJoiner(nop => (), Vector(1, 2), Vector(0, 1)))
      val node = actor.underlyingActor
      node.receive(Primary(prim))
      assert(node.primaryValues.get(Vector(16, 17)).get.contains(primaryVec))
      node.receive(Secondary(sec))
      assert(node.secondaryValues.contains(Vector(13, 15)))
    }
    "remove negative nodes" in {
      val prim1 = Vector(15, 16, 17, 18)
      val prim2 = Vector(9, 8, 7, 6)
      val prim = ChangeSet(
        positive = Vector(prim1, prim2)
      )
      val primRemove = ChangeSet(
        negative = Vector(prim1)
      )
      val sec1 = Vector(13, 15, 16, 17)
      val sec2 = Vector(1, 2, 3, 4)
      val sec = ChangeSet(
        positive = Vector(sec1, sec2)
      )

      val actor = TestActorRef(new HashAntiJoiner(nop => (), Vector(1, 2), Vector(0, 1)))
      val node = actor.underlyingActor
      node.receive(Primary(prim))
      node.receive(Secondary(sec))
      node.receive(Primary(ChangeSet(negative = Vector(prim1))))
      assert(node.primaryValues.get(Vector(16, 17)) == None)
      assert(node.primaryValues.get(Vector(8, 7)).get.contains(prim2))
      node.receive(Secondary(ChangeSet(negative = Vector(sec1))))
      assert(!node.secondaryValues.contains(Vector(13, 15)))
      assert(node.secondaryValues.contains(Vector(1, 2)))
    }
  }
  "do simple antijoins" in {
    val prim = ChangeSet(
      positive = Vector(Vector(15, 16, 17, 18), Vector(4, 5, 6, 7))
    )
    val sec = ChangeSet(
      positive = Vector(Vector(13, 15, 16))
    )
    val primarySel = Vector(0, 1)
    val secondarySel = Vector(1, 2)
    val echoActor = system.actorOf(TestActors.echoActorProps)
    val joiner = system.actorOf(Props(new HashAntiJoiner(echoActor ! _, primarySel, secondarySel)))

    joiner ! Secondary(sec)
    expectMsg(ChangeSet())
    joiner ! Primary(prim)
    expectMsg(ChangeSet(
      positive = Vector(Vector(4, 5, 6, 7))
    ))
  }
}