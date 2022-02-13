package services

import scalikejdbc._
import scalikejdbc.config._

import models.Data

object SqlService {
  DBs.setupAll()
  implicit val session: AutoSession = AutoSession

  // sanity check the DB is connected
  def checkDb():Boolean = {
    numImports.head >= 0
  }

  val numImports: Seq[Long] = DB readOnly { implicit session =>
    sql"select count(*) from scala_play_sandbox.importData".map(_.long(1)).list.apply()
  }

  // returns true if table was created
  def createTable(name:String, columns:List[String]):Boolean = {
    // if not exists to aid in testing
    // name is expected to be a unique hash
    val table = SQLSyntax.createUnsafely(name)
    val colList = SQLSyntax.createUnsafely(createColList(columns))
    sql"CREATE TABLE IF NOT EXISTS scala_play_sandbox.$table ($colList)".execute.apply()
  }

  // insert record into ImportData table
  def insertImportData(name:String):Boolean = {
    val inserted = sql"insert into scala_play_sandbox.importdata (filename) values ($name)".update.apply()
    if (inserted == 1) true
    else false
  }

  // returns true if data was inserted
  def insertRow(name:String, row:Map[String, String]):Boolean = {
    // building a list[String,String] which will be columnNames, values to build a SQL string
    val colRows = row.foldLeft((s"",s"")) {
      (acc, cur) => {
        // _1 is colName _2 is value
        (acc._1 + cur._1.replaceAll("[^a-zA-Z0-9]","") + ","
          ,acc._2 + "'" + cur._2 + "',")
      }
    }
    val table = SQLSyntax.createUnsafely(name)
    val colList = SQLSyntax.createUnsafely(colRows._1.dropRight(1))
    val valList = SQLSyntax.createUnsafely(colRows._2.dropRight(1))
    sql"INSERT INTO scala_play_sandbox.$table ($colList) VALUES ($valList)".execute.apply()
  }

  def createColList(columns:List[String]):String = {
    columns.foldLeft(s"") {
      (acc, cur) => {
        // removes all non alphanumeric characters from column names
        acc + cur.replaceAll("[^a-zA-Z0-9]","") + " VARCHAR, "
      }
    // remove the final two characters of comma space
    }.dropRight(2)
  }

}
