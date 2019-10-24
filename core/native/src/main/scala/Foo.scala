import java.io.File

import scala.compat.Platform
import com.github.sqlite4s._

/*import com.github.sqlite4s.c.util.CUtils
import scala.scalanative.unsafe.Zone
object Bytes2Hex {
  private val hexArray = "0123456789ABCDEF".toCharArray

  def convert(bytes: Array[Byte]): String = {
    val hexChars = new Array[Char](bytes.length * 2)

    var j = 0
    while (j < bytes.length) {
      val v = bytes(j) & 0xFF
      hexChars(j * 2) = hexArray(v >>> 4)
      hexChars(j * 2 + 1) = hexArray(v & 0x0F)

      j += 1
    }

    new String(hexChars)
  }
}*/


object Foo {

  implicit val sf: ISQLiteFactory = SQLiteFactory

  def main(args: Array[String]): Unit = {

    //println(Bar.a)

    println(s"Starting at ${Platform.currentTime}")

    /*Zone { implicit z =>
      val sql = "select x from x"
      val utf16Bytes = sql.getBytes(java.nio.charset.StandardCharsets.UTF_16)
      val utf16Sql = CUtils.bytesToCString(utf16Bytes)(z)
      // FIXME: add a nul-terminator offset seem to lead to invalid string => check why
      val utf16SqlLen = utf16Bytes.length //+ 1 // add nul-terminator to length (see above)

      println("utf16Bytes: " + Bytes2Hex.convert(utf16Bytes))
      println("fromCString:" + CUtils.fromCString(utf16Sql))
    }

    return*/

    /*Bar.create_dia_histogram(
      //new File("/mnt/d/Dev/wsl/ammonite/171121FFSWATH2000pg01.mzDB"),
      new File("/mnt/d/LCMS/quebec_dataset/UP_high_N4.raw.mzDB"),
      //new File("/mnt/d/LCMS/UPS1_50-5_small_runs/OVEMB150205_12.raw.mzDB"),
      new File("/mnt/d/Dev/wsl/scala-native/mzdb4s/dia_map_native.tsv"),
      0.1f
    )

    println(s"Ending at ${Platform.currentTime}")*/
  }


}
