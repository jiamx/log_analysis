# log_analysis
基于SparkStreaming的日志分析项目
&emsp;&emsp;基于SparkStreaming实现实时的日志分析，首先基于discuz搭建一个论坛平台，然后将该论坛的日志写入到指定文件，最后通过SparkStreaming实时对日志进行分析。
<!-- more -->

# 需求分析
- 统计指定时间段的热门文章
- 统计指定时间段内的最受欢迎的用户（以 ip 为单位）
- 统计指定时间段内的不同模块的访问量
# 项目架构
![](基于SparkStreaming的日志分析项目/项目架构.png)
# 代码分析
resources  
&emsp;&emsp;&emsp;&emsp;access_log.txt:日志样例  
&emsp;&emsp;&emsp;&emsp;log_sta.conf ：配置文件  
scala.com.jmx.analysis  
&emsp;&emsp;&emsp;&emsp;AccessLogParser.scala :日志解析  
&emsp;&emsp;&emsp;&emsp;logAnalysis：日志分析  
scala.com.jmx.util  
&emsp;&emsp;&emsp;&emsp;Utility.scala:工具类  

scala  
&emsp;&emsp;&emsp;&emsp;Run：驱动程序(main)  

具体代码详见[github](https://github.com/jiamx/log_analysis)
# 搭建discuz论坛
### 安装XAMPP
#### 下载
    wget https://www.apachefriends.org/xampp-files/5.6.33/xampp-linux-x64-5.6.33-0-installer.run
#### 安装
    # 赋予文件执行权限
    chmod u+x xampp-linux-x64-5.6.33-0-installer.run
    # 运行安装文件
    ./xampp-linux-x64-5.6.33-0-installer.run
#### 配置环境变量
&emsp;&emsp;将以下内容加入到 ~/.bash_profile  

    export XAMPP=/opt/lampp/
    export PATH=$PATH:$XAMPP:$XAMPP/bin

#### 刷新环境变量
    source ~/.bash_profile
#### 启动XAMPP
    xampp restart
### root用户密码和权限修改
    mysql –uroot # 安装的mysql的root用户默认没有密码，且只能本地登录
    # 修改root用户密码为123
    update mysql.user set password=PASSWORD('123') where user='root';
    flush privileges;
    # 赋予root用户远程登录权限
    grant all privileges on *.* to 'root'@'%' identified by '123' with grant option;
    flush privileges;
### 安装Discuz
#### 下载discuz
    wget http://download.comsenz.com/DiscuzX/3.2/Discuz_X3.2_SC_UTF8.zip
#### 安装
    # 删除原有的web应用
    rm -rf /opt/lampp/htdocs/*
    unzip Discuz_X3.2_SC_UTF8.zip –d /opt/lampp/htdocs/
    cd /opt/lampp/htdocs/
    mv upload/* .
    # 修改目录权限
    chmod 777 -R /opt/lampp/htdocs/config/
    chmod 777 -R /opt/lampp/htdocs/data/
    chmod 777 -R /opt/lampp/htdocs/uc_client/
    chmod 777 -R /opt/lampp/htdocs/uc_server/
### Discuz基本操作
#### 自定义版块


1. 进入discuz后台：http://slave1/admin.php


1. 点击顶部的“论坛”菜单


1. 按照页面提示创建所需版本，可以创建父子版块）
#### 查看访问日志
&emsp;&emsp;日志默认地址  
     `/opt/lampp/logs/access_log `   
&emsp;&emsp;实时查看日志命令  
    `tail –f /opt/lampp/logs/access_log`
### Discuz帖子/版块存储简介
    mysql -uroot -p123 ultrax # 登录ultrax数据库
    查看包含帖子id及标题对应关系的表
    # tid, subject（文章id、标题）
    select tid, subject from pre_forum_post limit 10;
    # fid, name（版块id、标题）
    select fid, name from pre_forum_forum limit 40;
### 修改日志格式
#### 登录安装网站的服务器
&emsp;&emsp;网站安装在slave1服务器上，并且是以root用户安装的，所以需要以root用户登录slave1服务器

#### 找到Apache配置文件
&emsp;&emsp;Apache配置文件名称为httpd.conf，所在目录为 /opt/lampp/etc/ ，完整路径为 /opt/lampp/etc/httpd.conf
#### 修改日志格式
&emsp;&emsp;关闭通用日志文件的使用  

    CustomLog "logs/access_log" common

&emsp;&emsp;启用组合日志文件  
  
    CustomLog "logs/access_log" combined

&emsp;&emsp;重新加载配置文件  
  
    xampp reload

&emsp;&emsp;检查访问日志  

    tail -f /opt/lampp/logs/access_log
### Flume与Kafka配置
&emsp;&emsp;Flume agent的配置文件为:  
    # agent的名称为a1
    a1.sources = source1
    a1.channels = channel1
    a1.sinks = sink1

    # set source
    #a1.sources.source1.type = spooldir
    a1.sources.source1.type = TAILDIR
    a1.sources.source1.filegroups = f1
    a1.sources.source1.filegroups.f1 = /opt/lampp/logs/access_log
    a1sources.source1.fileHeader = flase

    # set sink
    a1.sinks.sink1.type = org.apache.flume.sink.kafka.KafkaSink
    #a1.sinks.sink1.kafka.bootstrap.servers = master:9092,slave1:9092,slave2:9092
    a1.sinks.sink1.brokerList= master:9092,slave1:9092,slave2:9092
    a1.sinks.sink1.topic= aboutyunlog
    a1.sinks.sink1.kafka.flumeBatchSize = 20
    a1.sinks.sink1.kafka.producer.acks = 1
    a1.sinks.sink1.kafka.producer.linger.ms = 1
    a1.sinks.sink1.kafka.producer.compression.type = snappy

    # set channel
    a1.channels.channel1.type = file
    a1.channels.channel1.checkpointDir = /home/aboutyun/data/flume_data/checkpoint
    a1.channels.channel1.dataDirs= /home/aboutyun/data/flume_data/data

    # bind
    a1.sources.source1.channels = channel1
    a1.sinks.sink1.channel = channel1

&emsp;&emsp;创建所依赖目录  

    mkdir -p /home/aboutyun/data/flume_data/checkpoint
    mkdir -p /home/aboutyun/data/flume_data/data
### 创建MySQL数据库和所需要的表
#### 创建数据库
    CREATE DATABASE `statistics` CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';


#### 创建表
    1.	特定时间段内不同ip的访问次数：client_ip_access
    CREATE TABLE `client_ip_access` (
                            `client_ip` text COMMENT '客户端ip',
                             `sum` bigint(20) NOT NULL COMMENT '访问次数',
                             `time` text NOT NULL  COMMENT '统计时间'
                         ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

     2.	特定时间段内不同文章的访问次数：hot_article
    CREATE TABLE `hot_article` (
                           `article_id` text COMMENT '文章id',
                           `subject` text NOT NULL COMMENT '文章标题',
                           `sum` bigint(20) NOT NULL COMMENT '访问次数',
                          `time` text NOT NULL COMMENT '统计时间'
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

     3.	特定时间段内不同版块的访问次数：hot_section
    CREATE TABLE `hot_section` (
                      `section_id` text COMMENT '版块id',
                      `name` text NOT NULL COMMENT '版块标题',
                       `sum` bigint(20) NOT NULL COMMENT '访问次数',
                      `time` text NOT NULL COMMENT '统计时间'
                      ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

### 打包部署存在的问题
#### 问题1
    Exception in thread "main" java.lang.SecurityException: Invalid signature file digest for Manifest main attributes
#### 解决方式
    原因：
     使用sbt打包的时候导致某些包的重复引用，所以打包之后的META-INF的目录下多出了一些*.SF,*.DSA,*.RSA文件
    解决办法：
    删除掉多于的*.SF,*.DSA,*.RSA文件
    zip -d aboutyun_log_analysis_apache.jar META-INF/*.RSA META-INF/*.DSA META-INF/*.SF
#### 问题2
    Exception in thread "main" java.io.FileNotFoundException: File file:/data/spark_data/history/event-log does not exist
#### 解决方式
    原因：
    由于spark的spark-defaults.conf配置文件中配置 eventLog 时指定的路径在本机不存在。
    解决办法：
    创建对应的文件夹，并赋予对应权限
    sudo mkdir -p /data/spark_data/history/spark-events 
    sudo mkdir -p /data/spark_data/history/event-log 
    sudo chmod 777 -R /data

















