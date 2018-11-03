package onextent.akka.naviblob.azure.avro

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import onextent.akka.naviblob.akka.{NoMore, Pull}
import onextent.akka.naviblob.azure.storage.{LakeConfig, LakePaths}

object GzipConnector extends LazyLogging {

  val name: String = "GZipConnector"

  def props[T](implicit config: LakeConfig) = Props(new GzipConnector())
}

class GzipConnector(implicit config: LakeConfig)
    extends Actor
    with LazyLogging {

  val pathsIterator: Iterator[String] = new LakePaths().toList.iterator

  val firstPath: String = pathsIterator.next()
  logger.debug(s"reading from first path $firstPath")

  var readerIterator: Iterator[String] = new GZipReader(firstPath).read()

  override def receive: Receive = {

    case _: Pull =>
      if (readerIterator.hasNext) {
        // read one from the current file
        sender() ! readerIterator.next()
      } else {
        // open next file and read one
        if (pathsIterator.hasNext) {
          val nextPath = pathsIterator.next()
          logger.debug(s"reading from next path $nextPath")
          readerIterator = new GZipReader(nextPath).read()
          sender() ! readerIterator.next()
        } else {
          // all files in original path spec have been processed
          sender() ! NoMore()
        }
      }

    case x => logger.error(s"I don't know how to handle ${x.getClass.getName}")

  }

}