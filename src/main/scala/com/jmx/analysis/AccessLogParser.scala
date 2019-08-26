package com.jmx.analysis

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.{Matcher, Pattern}

/**
  *  @Created with IntelliJ IDEA.
  *  @author : jmx
  *  @Date: 2019/7/22
  *  @Time: 13:15
  *  */
//构建一个日志封装类
case class AccessLogRecord(
                            clientIpAddress: String, // 客户端ip地址
                            rfc1413ClientIdentity: String, // typically `-`
                            remoteUser: String, // typically `-`
                            dateTime: String, //日期，格式为[day/month/year:hour:minute:second zone]，如：[12/Jan/2018:21:03:09 +0800]
                            request: String, // url请求，如：`GET /foo ...`
                            httpStatusCode: String, // 状态码，如：200, 404.
                            bytesSent: String, // 传输的字节数，有可能是 `-`
                            referer: String, // 参考链接
                            userAgent: String // 浏览器和操作系统类型
                          )

@SerialVersionUID(100L)
class AccessLogParser extends Serializable {
  //构建正则表达式
  private val ddd = "\\d{1,3}" // at least 1 but not more than 3 times (possessive)
  private val ip = s"($ddd\\.$ddd\\.$ddd\\.$ddd)?" // like `123.456.7.89`
  private val client = "(\\S+)" // '\S' is 'non-whitespace character'
  private val user = "(\\S+)"
  private val dateTime = "(\\[.+?\\])" // like `[21/Jul/2009:02:48:13 -0700]`
  private val request = "\"(.*?)\"" // any number of any character, reluctant
  private val status = "(\\d{3})"
  private val bytes = "(\\S+)" // this can be a "-"
  private val referer = "\"(.*?)\""
  private val agent = "\"(.*?)\""
  private val regex = s"$ip $client $user $dateTime $request $status $bytes $referer $agent"
  private val p = Pattern.compile(regex) //  Compiles the given regular expression into a pattern.

  /*
  *构造访问日志的封装类对象
  * */
  private def buildAccessLogRecord(matcher: Matcher) = {
    AccessLogRecord(
      matcher.group(1),
      matcher.group(2),
      matcher.group(3),
      matcher.group(4),
      matcher.group(5),
      matcher.group(6),
      matcher.group(7),
      matcher.group(8),
      matcher.group(9)

    )

  }

  /**
    *
    * @param record ，record 一条apache combined 日志
    * @return 一个 Some(AccessLogRecord) 实例或者None
    *         解析日志记录，将解析的日志封装成一个AccessLogRecord类
    */
  def parseRecord(record: String): Option[AccessLogRecord] = {
    val matcher = p.matcher(record)
    if (matcher.find()) {
      Some(buildAccessLogRecord(matcher))
    }
    else {
      None
    }


  }

  /**
    *
    * @param request url请求，类型为字符串，类似于 "GET /the-uri-here HTTP/1.1"
    * @return 一个三元组(requestType, uri, httpVersion). requestType表示请求类型，如GET, POST等
    */
  def parseRequestField(request: String): Option[Tuple3[String, String, String]] = {
    //请求的字符串格式为：“GET /test.php HTTP/1.1”，用空格切割
    val arr = request.split(" ")
    if (arr.size == 3)
      Some((arr(0), arr(1), arr(2)))
    else
      None
  }

  /**
    * 将apache日志中的英文日期转化为指定格式的中文日期
    *
    * @param dateTime     传入的apache日志中的日期字符串，"[21/Jul/2009:02:48:13 -0700]"
    * @param inputFormat  输入的英文日期格式
    * @param outPutFormat 输出的日期格式
    * @return
    */
  def parseDateField(dateTime: String, inputFormat: String = "dd/MMM/yyyy:HH:mm:ss", outPutFormat: String = "yyyy-MM-dd HH:mm:ss"): Option[String] = {
    val dateRegex = "\\[(.*?) .+]"
    val datePattern = Pattern.compile(dateRegex)
    val dateMatcher = datePattern.matcher(dateTime)
    if (dateMatcher.find()) {
      val dateString = dateMatcher.group(1)
      var dateFormat = new SimpleDateFormat(inputFormat, Locale.ENGLISH)
      val date = dateFormat.parse(dateString)
      dateFormat = new SimpleDateFormat(outPutFormat)
      Some(dateFormat.format(date))
    } else (None)
  }
  def parseSectionIdAndArticleId(request: String):Tuple2[String,String]={
    val sectionIdRegex = "(?<=forumdisplay&fid=)\\d+".r // 匹配出前面是"forumdisplay&fid="的数字记为版块id
    val articleIdRegex = "(?<=tid=)\\d+".r // 匹配出前面是"tid="的数字记为文章id

    val arr = request.split(" ")
    var sectionId = ""
    var articleId = ""
    if (arr.size == 3) {
      sectionId = sectionIdRegex.findFirstIn(arr(1)).getOrElse("")
      articleId = articleIdRegex.findFirstIn(arr(1)).getOrElse("")
    }
    (sectionId, articleId)

  }

}
