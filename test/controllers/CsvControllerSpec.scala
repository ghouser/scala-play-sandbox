package controllers

import models.Data
import org.scalatestplus.play.PlaySpec
import java.io.File

class CsvControllerSpec extends PlaySpec {

  val testFile = new File("test/resources/simpleTest.csv")
  "parseUtils" should {
    "open a text file and create a List[String] from the contents" in {
      val list = parseUtils.fileToList(Option(testFile))
      val expected = Some(List("header1,header2,header3", "value1,value2,value3", "value21,value22,value33", "value31,value32,value33", "valueError"))
      list mustBe expected
    }
    "parse a string into a List[String] by splitting on comma and accounting for double quoted strings" in {
      val list = parseUtils.stringFilter("test,\"test,test\",t\"es\"t,\"t\"es\"t\"")
      val expected = List("test","test,test","t\"es\"t","t\"es\"t")
      list mustBe expected
    }
    "parse the first row from a List[String] to find the header row" in {
      val list = parseUtils.fileToList(Option(testFile))
      val header = parseUtils.parseHeader(list.get)
      val expected = List("header1","header2","header3")
      header mustBe expected
    }
    "parse a string into a List[String] and return true for expected length" in {
      val checkRow = parseUtils.parseRow("value,value,value",3)
      checkRow._1 mustBe List("value","value","value")
      checkRow._2 mustBe true
    }
    "parse a string into a List[String] and return false for unexpected length" in {
      val checkRow = parseUtils.parseRow("value,value,val\"ue",3)
      checkRow._1 mustBe List("value,value,val\"ue")
      checkRow._2 mustBe false
    }
    "parse a List[String] into a data.Result type" in {
      val l:List[String] = List("header1,header2,header3",
                                "value1,value2,value3",
                                "value21,value22,value23",
                                "value31,value32,value33",
                                "valueError")
      val result:Data.Reply = parseUtils.parseRows(l,"test")
      val expected:Data.Reply = Data.Reply(
        Data.Metadata("csv_98f6bcd4621d373cade4e832627b4f6",4)
        ,List(Map("header1" -> "value1","header2" -> "value2","header3" -> "value3")
          ,Map("header1" -> "value21","header2" -> "value22","header3" -> "value23")
          ,Map("header1" -> "value31","header2" -> "value32","header3" -> "value33"))
        ,List(Map("row" -> "4", "value" -> "valueError"))
      )
      result mustBe expected
    }
  }

}
