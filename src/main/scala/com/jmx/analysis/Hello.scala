package com.jmx.analysis

/**
  *  @Created with IntelliJ IDEA.
  *  @author : jmx
  *  @Date: 2019/7/23
  *  @Time: 13:29
  *  */
object Hello {
  /*  //构建正则表达式
    private val ddd = "\\d{1,3}" // at least 1 but not more than 3 times (possessive)
    private val ip = s"($ddd\\.$ddd\\.$ddd\\.$ddd)?" // like `123.456.7.89`
    private val client = "(\\S+)" // '\S' is 'non-whitespace character'
    private val user = "(\\S+)"
    private val dateTime = "(\\[.+?\\])" // like `[21/Jul/2009:02:48:13 -0700]`
    private val request = "\"(.*)\"" // any number of any character, reluctant
    private val status = "(\\d{3})"
    private val bytes = "(\\S+)" // this can be a "-"
    private val referer = "\"(.*)\""
    private val agent = "\"(.*)\""
    private val regex = s"$ip $client $user $dateTime $request $status $bytes $referer $agent"
    private val p = Pattern.compile(regex)*/
  def main(args: Array[String]) = {
    /* val str ="-"
     val p = Pattern.compile("\"(.*?)\"")
     val m = p.matcher(str)
     println(m.groupCount())
     if(m.find()){
       println("Group 0:"+m.group(0))
       println("Group 0:"+m.group(1))*/
    /*val dateTime = "[07/Dec/2017:19:36:27 +0800]"
        val dateRegex = "\\[(.*?) .+]"
        val datePattern = Pattern.compile(dateRegex)
        val dateMatcher = datePattern.matcher(dateTime)
        val inputFormat = "dd/MMM/yyyy:HH:mm:ss"
        val outPutFormat = "yyyy-MM-dd HH:mm:ss"
        if (dateMatcher.find()) {
          val dateString = dateMatcher.group(1)
          println(dateString)
          val dateFormat1 = new SimpleDateFormat(inputFormat, Locale.ENGLISH)
          val date = dateFormat1.parse(dateString)
          println(date)
          val dateFormat2 = new SimpleDateFormat(outPutFormat)
          val date1 = dateFormat2.format(dateString)
          println(date1)*/


    //  }
    val arrs = "/about/forum.php?mod=viewthread&tid=5&extra=page%3D1"
    val sectionIdRegex = "(?<=forumdisplay&fid=)\\d+".r // 匹配出前面是"forumdisplay&fid="的数字记为版块id
    val articleIdRegex = "(?<=tid=)\\d+".r // 匹配出前面是"tid="的数字记为文章id

    var sectionId = ""
    var articleId = ""

    sectionId = sectionIdRegex.findFirstIn(arrs).getOrElse("")
    println(sectionId)
    val a = sectionIdRegex.findAllIn(arrs)
    val b = sectionIdRegex.findAllMatchIn(arrs)
    val c = sectionIdRegex.findFirstMatchIn(arrs)
    println("a= " + a + " b= " + b +" c= " +c)


    articleId = articleIdRegex.findFirstIn(arrs).getOrElse("")
    println(articleId)
    val a1 = sectionIdRegex.findAllIn(arrs)
    val b1 = sectionIdRegex.findAllMatchIn(arrs)
    val c1 = sectionIdRegex.findFirstMatchIn(arrs)
    println(" a1= " + a1 + " b1= " + b1 +" c1= " +c1)

  }


}
