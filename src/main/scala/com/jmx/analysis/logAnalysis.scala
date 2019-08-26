package com.jmx.analysis

import java.text.SimpleDateFormat
import java.util.{Date, Properties}

import com.typesafe.config.Config
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.streaming.dstream.DStream

/**
  *  @Created with IntelliJ IDEA.
  *  @author : jmx
  *  @Date: 2019/7/25
  *  @Time: 22:46
  *  */
class logAnalysis(config: Config, windowStream: DStream[String]) {
  //解析业务数据库的url参数
  val mysqlForumUrl = config.getString("mysql.forum_url")
  //解析存放统计数据的数据库URL
  val statistics_url = config.getString("mysql.statistics_url")
  /**
    * 目标：将Dstream流转换为DataFrame
    * （1）先将DStream转换为Row类型的数据
    * （2）构建schema
    * （3）创建DataFrame
    *
    */

  //过滤无效的日志记录
  val avaliableLog = logAnalysis.getAvailableAccessLog(windowStream)
  //将日志转换为row类型的格式
  val rowLogDS = logAnalysis.transformDataWithRow(avaliableLog)
  //构建schema
  val schema = StructType(
    StructField("client_ip", StringType, true) ::
      StructField("datetsring", StringType, true) ::
      StructField("section_id", StringType, true) ::
      StructField("article_id", StringType, true) :: Nil

  )

  //创建sparksession
  val spark = SparkSession
    .builder()
    .appName("log analysis")
    .getOrCreate()

  /**
    * 日志分析方法，在Run类中调用该方法
    * 该方法封装了处理日志的逻辑
    * 1.统计热门板块
    * 2.统计热门文章
    * 3.统计不同ip对板块和文章的访问量
    */
  def analysis() = {
    //读取业务数据库数据，加载pre_forum_forum表
    //该表为板块id与板块标题的信息
    val forumDF = spark.read.format("jdbc")
      .option("driver", "com.mysql.jdbc.Driver")
      .option("url", mysqlForumUrl)
      .option("dbtable", "pre_forum_forum")
      .load()
    //将DataFrame注册为临时视图
    /**
      * Creates a local temporary view using the given name. The lifetime of this
      * temporary view is tied to the [[SparkSession]] that was used to create this Dataset.
      */
    forumDF.createOrReplaceTempView("pre_forum_forum")

    //读取业务数据库数据，加载pre_forum_post表
    //该表为文章id与文章标题的信息
    val postDF = spark.read.format("jdbc")
      .option("driver", "com.mysql.jdbc.Driver")
      .option("url", mysqlForumUrl)
      .option("dbtable", "pre_forum_post")
      .load()
    //注册临时视图
    postDF.createOrReplaceTempView("pre_forum_post")
    rowLogDS.foreachRDD {
      rowrdd =>
        val nowDate = new Date()
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentTimeStamp = dateFormat.format(nowDate)
        println(s"current TimeStamp is $currentTimeStamp")
        //将RDD转换为DataFrame
        val rawLogDF = spark.createDataFrame(rowrdd, schema)
        //将DataFramezhuce为临时视图
        rawLogDF.createOrReplaceTempView("user_access_log")
        rawLogDF.show(1)

        val prop = new Properties()
        prop.put("driver", "com.mysql.jdbc.Driver")
        //统计热门板块
        val hot_section_sql =
          """select a.section_id, b.name, count(1) as sum, '%s' as time
            |from user_access_log a
            |join pre_forum_forum b
            |on a.section_id = b.fid
            |where a.section_id <> ''
            |group by a.section_id, b.name
            |order by count(1) desc
            |limit 10
          """.stripMargin.format(currentTimeStamp )

        val hotSection = spark.sql(hot_section_sql)
        hotSection.show(1)
        //将结果保存到hot_section表中，写入的模式为Append
        hotSection.write.mode(SaveMode.Append).jdbc(statistics_url, "hot_section", prop)

        //统计热门文章
        val hot_article_sql =
          """select a.article_id,b.subject,count(1) as sum,'%s' as time
                        |from user_access_log a
                        |join pre_forum_post b
                        | on a.article_id = b.tid
                        | where a.article_id <> ''
                        | group by a.article_id,b.subject
                        | order by count(1) desc
                        | limit 10

          """.stripMargin.format(currentTimeStamp)
        val hotArticle = spark.sql(hot_article_sql)
        //      hotArticle.show()
        hotArticle.write.mode(SaveMode.Append).jdbc(statistics_url, "hot_article", prop)

        // 统计不同ip对版块和文章的访问量
        val clientIPAccess = spark.sql(
          """
            |select client_ip, count(1) as sum, '%s' as time
            |from user_access_log
            |where article_id <> '' or section_id <> ''
            |group by client_ip
            |order by count(1) desc
          """.stripMargin.format(currentTimeStamp))
        //      clientIPAccess.show()
        clientIPAccess.write.mode(SaveMode.Append).jdbc(statistics_url, "client_ip_access", prop)

    }

  }

}


object logAnalysis {
  private val nullRow = Row("0.0.0.0", "0000-00-00 00:00:00", "0", "0")

  /**
    * 筛选可用的日志记录
    *
    * @param accessLog
    * @return
    */
  def getAvailableAccessLog(accessLog: DStream[String]): DStream[Option[AccessLogRecord]] = {
    val parser = new AccessLogParser()
    //解析原始日志，将其解析为AccessLogRecord格式
    val parseDS = accessLog.map(line => parser.parseRecord(line))
    //过滤掉无效日志
    //Returns true if the option is an instance of $some, false otherwise.
    //过滤掉空记录的日志
    val availableAccessLog = parseDS.filter(record => record.isDefined)
      //过滤掉状态码非200的记录，即保留请求成功的日志记录
      .filter(record => record.get.httpStatusCode.toInt == 200)
    availableAccessLog

  }

  /**
    * 将日志记录转换成row类型的格式
    *
    * @param accessLog
    * @return
    */
  def transformDataWithRow(accessLog: DStream[Option[AccessLogRecord]]): DStream[Row] = {

    accessLog.map {
      logRecord =>
        val parser = new AccessLogParser()
        //客户端ip地址
        val clientIp = logRecord.get.clientIpAddress
        //请求的时间
        val datetime = logRecord.get.dateTime
        //请求的方式和资源
        val request = logRecord.get.request
        //解析请求的时间
        val dateString = parser.parseDateField(datetime).getOrElse("")
        //解析请求的方式与资源
        val sectionIdAndArticleId = parser.parseSectionIdAndArticleId(request)

        if (dateString == "" || sectionIdAndArticleId == ("", "")) {
          nullRow
        } else {
          Row(clientIp, dateString, sectionIdAndArticleId._1, sectionIdAndArticleId._2)
        }

    }

  }

}
