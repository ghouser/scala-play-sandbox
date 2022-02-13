package controllers
// play imports
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._
// scala and java imports
import javax.inject._
import scala.io.{BufferedSource, Source}
import java.io.File
import java.security.MessageDigest
import java.math.BigInteger
// local imports
import models.Data
import services.SqlService

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
    // parse file into List of Strings
    val lines:Option[List[String]] = parseUtils.fileToList(file)

    // confirm file parsed, then continue processing
    lines match {
      case None => InternalServerError("File failed to upload and parse")
      case Some(l) =>
        // Sanity check there are multiple lines
        if(l.length < 2) InternalServerError("File is not longer than 1 row")
        // parse and reply
        else {
          val reply = parseUtils.parseRows(l,file.get.toString)
          val insertImportData = services.SqlService.insertImportData(reply.metadata.importName)
          val createSQLTable = services.SqlService.createTable(reply.metadata.importName, parseUtils.parseHeader(l))
          val insertRows = reply.rows.map(r => services.SqlService.insertRow(reply.metadata.importName, r))
          if(!createSQLTable) InternalServerError("CSV was parsed but failed to creat import SQL table" + Json.toJson(reply))
          if(insertRows.contains(false)) InternalServerError("CSV was parsed but all records did not write out to SQL table" + Json.toJson(reply))
          if(!insertImportData) InternalServerError("CSV was parsed but run was not recorded in importData table" + Json.toJson(reply))
          Ok(Json.toJson(reply))
        }
    }
  }

  // pings DB to see if it's connected
  def checkDB(): Action[AnyContent] = Action { request: Request[AnyContent] =>
    if (SqlService.checkDb()) Ok("Confirmed connection to SQL Database")
    else InternalServerError("Not Connected to SQL Database")
  }
}

object parseUtils {
  // makes a GUID based on a string
  def md5Hash(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1,digest)
    val hashedString = bigInt.toString(16)
    s"csv_$hashedString"
  }

  // logic to help sanity check files
  val commaRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"
  val quoteRegex = "^\\\"|\\\"$"

  // opens file and converts to List[String]
  def fileToList (file:Option[File]):Option[List[String]] = {
    try {
      val validFile = file.get
      val bufferedSource: BufferedSource = Source.fromFile(validFile)
      // loop through file to generate an iterator and convert to list
      val lineIterator:Iterator[String] = for (line <- bufferedSource.getLines()) yield line
      // return list
      Option(lineIterator.toList)
    } catch {
      case e: Exception => None
    }
  }

  def stringFilter (s:String):List[String] = {
    // split the row, trim each element, convert to list, replace double quote wrapping
    s.split(commaRegex).map(_.trim).toList.map(_.replaceAll(quoteRegex,""))
  }

  // takes the List[String] and parses the first row to create the header
  def parseHeader (csv: List[String]):List[String] = {
    stringFilter(csv.head)
  }

  // parses the row, returns a list of string and if row parsed without error
  def parseRow (row: String, length: Int):(List[String],Boolean) = {
    val rowList = stringFilter(row)
    // if rowList is not the expected length, return false
    if (rowList.length != length){
      (rowList,false)
    }
    else (rowList,true)
  }

  def parseRows (csv: List[String], fileName:String):Data.Reply = {
    val header = parseHeader(csv)
    val csvIndex = csv.zipWithIndex
    // create the metadata row
    // md5hash of filename should be unique temp files are given random names
    // number of records is csv.length-1 because of header row
    val metadata = Data.Metadata(md5Hash(fileName),csv.length-1)
    val emptyReply:Data.Reply = Data.Reply(
      metadata
      ,List.empty[Map[String, String]]
      ,List.empty[Map[String, String]]
    )

    if(header.nonEmpty){
      // tail will skip the first row, fold left creates an accumulator to build output
      csvIndex.tail.foldLeft(emptyReply) {
        (acc, cur) => {
          // zip _1 is value _2 is index
          val checkRow = parseRow(cur._1,header.length)
          // if check row is true, add to rows part of reply
          if (checkRow._2) Data.Reply(metadata,acc.rows ++ List((header zip checkRow._1).toMap),acc.errors)
          // if check row is false, add to errors part of reply
          else Data.Reply(metadata,acc.rows,acc.errors ++ List(Map("row"->cur._2.toString,"value"->cur._1)))
        }
      }
    }
    else emptyReply
  }

}
