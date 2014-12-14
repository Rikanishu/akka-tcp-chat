package com.example.akkaTcpChat

import akka.serialization._
import akka.actor.ActorSystem
import scala.Array
import scala.collection.mutable
import akka.util.ByteString
import scala.util.Try

object Common {

  val CLIENT_MESSAGE = "ClientMessage"
  val CLIENT_INIT = "ClientInit"
  val OTHER_CLIENT_MESSAGE = "OtherClientMessage"

  object Request {

    def deserializeFromByteString(bytes: ByteString)(implicit system: ActorSystem): Try[Request] = {
      Common.deserialize(bytes.toArray, classOf[Request])
    }

  }

  class Request(name: String) extends mutable.HashMap[String, Any] {

    request = name

    def request = {
      this("requestName")
    }

    def request_=(name: String) {
      this("requestName") = name
    }

    def serializeAsByteString(implicit system: ActorSystem): Try[ByteString] = {
      Common.serialize(this).flatMap((b: Array[Byte]) => {
        Try(ByteString(b))
      })
    }

  }

  class SerializationError extends Exception

  def serialize(obj: AnyRef)(implicit system: ActorSystem) = {
    val serialization = SerializationExtension(system)
    serialization.serialize(obj)
  }

  def deserialize[T](bytes: Array[Byte], cls: Class[T])(implicit system: ActorSystem) = {
    val serialization = SerializationExtension(system)
    serialization.deserialize(bytes, cls)
  }

}
