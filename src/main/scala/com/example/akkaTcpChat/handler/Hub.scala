package com.example.akkaTcpChat.handler

import akka.actor.{ActorRef, Actor}
import akka.io.Tcp
import akka.event.Logging
import scala.collection.mutable

class Hub extends Actor {

  var connections = mutable.Map[String, ActorRef]()
  val log = Logging(context.system, this)

  def receive = {
    case Hub.Register(remote, connection) =>
      val peer = context.actorOf(Peer.props(remote, connection))
      connection ! Tcp.Register(peer)
      connections += (remote -> peer)

    case Hub.Broadcast(senderId, name, message) =>
      log.debug(senderId + " run broadcast")
      connections.foreach {
        case (`senderId`, _) =>
        case (rem, peer) => peer ! Peer.PeerMessage(name, message)
      }

    case Hub.Unregister(senderId) =>
      log.debug(senderId + " close connection")
      connections.remove(senderId)
  }

}

object Hub {

  case class Register(remote: String, connection: ActorRef)
  case class Broadcast(senderId: String, name: String, message: String)
  case class Unregister(senderId: String)

}