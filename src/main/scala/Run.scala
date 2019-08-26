import com.jmx.analysis.logAnalysis
import com.jmx.util.Utility
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  *  @Created with IntelliJ IDEA.
  *  @author : jmx
  *  @Date: 2019/7/25
  *  @Time: 22:01
  *  */
object Run {
  def main(args: Array[String]): Unit = {
    //传入参数
    val configPath = args(0)
    println(s"configPath : $configPath")
    val config = Utility.parseConf(configPath)
    println(s"config: $config")

    val batchPeriod = config.getInt("kafka.batch_period")
    val windowPeriod = config.getInt("kafka.window_period")
    val slidePeriod = config.getInt("kafka.slide_period")
    //将kafka主题封装成set集合
    val topicSet = Set(config.getString("kafka.topic").trim)
    val brokers = config.getString("kafka.broker")
    println(s"brokers:$brokers")
    //创建sparkconf对象
    val conf = new SparkConf().setAppName("analysis_log_task")
    //创建sparkContext
    val sc = new SparkContext(conf)
    //创建sparkStreaming
    val ssc = new StreamingContext(sc, Seconds(batchPeriod))
    //封装kafka参数
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> brokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "log"

    )
    //使用基于Driect的方式创建DStream流
    /**
      * locationStrategy In most cases, pass in [[LocationStrategies.PreferConsistent]],
      *                         see [[LocationStrategies]] for more details.
      * consumerStrategy In most cases, pass in [[ConsumerStrategies.Subscribe]],
      *                         see [[ConsumerStrategies]] for more details
      * K type of Kafka message key
      * V type of Kafka message value
      */
    val stream = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String,String](topicSet, kafkaParams)).map(_.value())//只取value值

    //对流使用滑动窗口操作

   val windowStream =  stream.window(Seconds(windowPeriod),Seconds(slidePeriod))
    println(windowStream)
    //调用分析日志的类，该类封装了处理日志的方法和逻辑
    val logAnalysis = new logAnalysis(config,windowStream)
    logAnalysis.analysis()

    ssc.start()
    ssc.awaitTermination()
  }

}
