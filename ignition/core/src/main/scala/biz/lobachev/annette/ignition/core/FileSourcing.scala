package biz.lobachev.annette.ignition.core

import org.slf4j.Logger
import play.api.libs.json.{Json, Reads}

import scala.util.{Failure, Success, Try}
import scala.io.Source

protected trait FileSourcing {
  protected val log: Logger

  protected def getData[A](name: String, filename: String)(implicit reads: Reads[A]): Either[Throwable, A] = {
    val jsonTry = Try(Source.fromResource(filename).mkString)
    jsonTry match {
      case Success(json) =>
        val resTry = Try(Json.parse(json).as[A])
        resTry match {
          case Success(seq) => Right(seq)
          case Failure(th)  =>
            val message = s"Parsing $name json failed: $filename"
            log.error(message, th)
            Left(new IllegalArgumentException(message, th))
        }
      case Failure(th)   =>
        val message = s"$name file load failed: $filename"
        log.error(message, th)
        Left(new IllegalArgumentException(message, th))

    }
  }
}
