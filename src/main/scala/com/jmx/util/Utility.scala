package com.jmx.util

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
  *  @Created with IntelliJ IDEA.
  *  @author : jmx
  *  @Date: 2019/7/25
  *  @Time: 21:25
  *  */
object Utility {
  /** *
    * 根据路径中的文件创建一个 Config 对象
    *
    * @param filePath 文件
    * @return
    */
  def parseConf(filePath:String):Config={

    val conf = ConfigFactory.parseFile(new File(filePath))
    conf

  }

  /**
    * 测试
    * @param args
    */
  def main(args: Array[String]): Unit = {

    val filePath = "E:\\IdeaProjects\\log_analysis\\src\\main\\resources\\log_sta.conf"
    val config = Utility.parseConf(filePath)
    println(config)
    println(config.getInt("kafka.batch_period"))
    println(config.getString("mysql.forum_url"))
    println(config.getString("kafka.topic"))
    val a = Set(config.getString("kafka.topic"))
  println(config.getString("kafka.broker".trim))
    println(config.getString("kafka.broker"))
  }

}
