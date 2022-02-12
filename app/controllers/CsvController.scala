package controllers

import play.api.libs.Files
import play.api.mvc._

import javax.inject._
import scala.io.{BufferedSource, Source}
import models.data
import play.api.libs.json.Json

@Singleton
class CsvController @Inject() (val controllerComponents: ControllerComponents)
extends BaseController {
  // the main endpoint, parseCsv
  def parseCsv(): Action[AnyContent] = Action { request: Request[AnyContent] =>
    val body: AnyContent = request.body
    val formEncoded: Option[MultipartFormData[Files.TemporaryFile]] = body.asMultipartFormData
    // get the file from the POST call
    val file = formEncoded.get.file("upload").map {
      tempFile =>
        tempFile.ref.toFile
    }
    // open file
    val bufferedSource: BufferedSource = Source.fromFile(file.get)
    // loop through file to generate an iterator and convert to list
    val lineIterator:Iterator[String] = for (line <- bufferedSource.getLines) yield line
    val lines:List[String] = lineIterator.toList
    // close file
    bufferedSource.close()

    // Sanity check there are multiple lines
    if(lines.length < 2) InternalServerError("File is not longer than 1 row")
    // parse and reply
    else {
      val reply = parseUtils.parseRows(lines)
      Ok(Json.toJson(reply))
    }
  }
}

object parseUtils {
  // logic to help sanity check files
  val commaRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"

  def parseHeader (csv: List[String]):List[String] = {
    csv.head.split(commaRegex).map(_.trim).toList
  }

  def parseRows (csv: List[String]):data.Reply = {
    val header = parseHeader(csv)
    val csvIndex = csv.zipWithIndex
    if(header.nonEmpty){
      // tail will skip the first row, fold left creates an accumulator to build output
      csvIndex.tail.foldLeft(data.Reply(List.empty[Map[String, String]],List.empty[Map[String, String]])) {
        (acc, cur) => {
          // zip _1 is value _2 is index
          val checkRow = parseRow(cur._1,header.length)
          // if check row is true, add to rows part of reply
          if (checkRow._2) data.Reply(acc.rows ++ List((header zip checkRow._1).toMap),acc.errors)
          // if check row is false, add to errors part of reply
          else data.Reply(acc.rows,acc.errors ++ List(Map(cur._2.toString -> cur._1)))
        }
      }
    }
    else data.Reply(List.empty[Map[String, String]],List.empty[Map[String, String]])
  }

  // parses the row, returns a list of string and if row parsed without error
  def parseRow (row: String, length: Int):(List[String],Boolean) = {
    val rowList = row.split(commaRegex).map(_.trim).toList
    // if rowList is not the expected length, return false
    if (rowList.length != length){
      (rowList,false)
    }
    else (rowList,true)
  }

}
