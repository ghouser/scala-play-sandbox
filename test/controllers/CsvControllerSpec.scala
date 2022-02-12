package controllers

import controllers.CsvController
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
    "parse the first row from a List[String] to find the header row" in {
      val list = parseUtils.fileToList(Option(testFile))
      val header = parseUtils.parseHeader(list.get)
      val expected = List("header1","header2","header3")
      header mustBe expected
    }
  }

}
