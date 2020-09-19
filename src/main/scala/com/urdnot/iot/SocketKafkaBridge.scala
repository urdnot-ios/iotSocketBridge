package com.urdnot.iot

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Framing, Source, Tcp}
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import scala.concurrent.{ExecutionContextExecutor, Future}

object SocketKafkaBridge extends App with LazyLogging {
  private implicit val system: ActorSystem = ActorSystem("iot_processor")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = materializer.executionContext
  private val envConfig: Config = system.settings.config.getConfig("env")
  private val kafkaProducerConfig = system.settings.config.getConfig("akka.kafka.producer")
  private val producerSettings =
    ProducerSettings(kafkaProducerConfig, new StringSerializer, new StringSerializer)
  private val kafkaProducer: Producer[String, String] = producerSettings.createKafkaProducer()

  private val log: Logger = Logger("socketBridge")
  private val host = envConfig.getString("socket.host")
  private val port = envConfig.getInt("socket.port")
  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind(host, port)
    connections runForeach { connection ⇒
      val kafkaMessage: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
        .map(_.utf8String)
        .map(socketToKafka)
        .map(ByteString(_))
      connection.handleWith(kafkaMessage)
    }

  // {"timestamp":1600537049,"host": "huzzah02","bme280": {"tempC":23.97,"tempF":75.15,"PressurePa":100822.81,"PressureInHg":29.77,"altitudeM":41.89},"SGP30": {"TVOCPPB":919,"eCO2PPM":825},"TSL2591": {"Vis":13,"IR":17}}
  def socketToKafka(message: String): Int = {
    val kafkaMessage = new ProducerRecord[String, String](envConfig.getString("kafka.topic"), message)
    kafkaProducer.send(kafkaMessage) match {
      case e: Exception => log.error("unable to send message: " + message + "because -- " + e.getMessage)
        0
      case _ => log.debug(message)
        1
    }
  }
}
