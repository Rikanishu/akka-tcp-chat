package com.example.akkaTcpChat

import akka.actor.{ Props, Actor }
import akka.io.{ IO, Tcp }
import akka.io.Tcp._
import java.net.InetSocketAddress
import akka.io.Tcp.Connected
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Bind
import akka.io.Tcp.Bound
import com.example.akkaTcpChat.handler.{Hub, Peer}
import akka.event.Logging

object Server {

  def props(host: InetSocketAddress): Props = {
    Props(new Server(host))
  }

}

class Server(host: InetSocketAddress) extends Actor {

  import context.system

  val log = Logging(context.system, this)

  IO(Tcp) ! Bind(self, host)
  val hub = context.actorOf(Props[Hub])

  def receive  = {
    case b @ Bound(localAddress) =>
      log.debug("Spawned " + localAddress.toString)

    case CommandFailed(_:Bind) =>
      println("Command failed. Going to stop server.")
      context stop self

    case c @ Connected(remote, local) =>
      log.debug("Connected " + remote.toString)
      hub ! Hub.Register(remote.toString, sender)
  }

}
