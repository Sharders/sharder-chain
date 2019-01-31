# Sharder #

## What is Sharder ##
- Sharder is a blockchain-based multi-chain storage Network.

## Project Structure ##
Java project，UI uses the vue.

    sharder-chain           // project root 
        |- classes/         // compiled class files 
        |- conf/            // configuration files 
        |- lib/             // 3rd or required library 
        |- html/            // compiled html file of ui 
        |- logs/            // logs 
        |- sharder_*_db/    // database of sharder-chain
        |- src/             // source code of sharder-chain
        |- ui/              // source code of ui
        |- run.sh           // shell for linux and osx 
        |- run.bat          // bat for windows 
        |- 3RD-PARTY-LICENSES  
        |- LICENSE 
        |- README.md 
NOTE：If you specify the compilation folder manually, you must set the compilation folder as above classes folder under the project root.  

## Run Sharder client ##
- TODO

## Start mine ##
Please read the [MINER-GUIDE.md](./MINER-GUIDE.md)

## Follow us ##
  - Website: https://sharder.org
  - Twitter: @SharderChain
  - Telegram: https://t.me/sharder_talk
  - Medium: https://medium.com/@SharderChain
  - Github: https://github.com/Sharders/sharder-chain
  - Wechat: supersharder
----

# 豆匣 #

## 豆匣是什么? ##
- 豆匣是多链架构区块链存储网络。

## 工程说明 ##
Java工程，UI使用vue。

    sharder-chain           // 工程根目录 
        |- classes/         // 编译后的源文件 
        |- conf/            // 配置文件
        |- lib/             // 第三方或则依赖的代码库
        |- html/            // 编译后的UI文件
        |- logs/            // 日志 
        |- sharder_*_db/    // 数据库
        |- src/             // 源代码
        |- ui/              // UI源代码
        |- run.sh           // linux和osx操作系统的启动脚本 
        |- run.bat          // windows操作系统的启动脚本
        |- 3RD-PARTY-LICENSES  
        |- LICENSE 
        |- README.md 
注意：如果你手动指定编译目录的话，需要将编译目录设置到上面的classes文件夹，否则run.sh和run.bat无法正常运行。

## 运行豆匣客户端 ##
你可以采用下面两种方式运行豆匣客户端：
* 方式一
    
        1. 在[官网](https://sharder.org)下载对应操作系统的客户端
        2. 安装豆匣客户端
        3. 运行豆匣客户端
* 方式二
    
        1. 将本工程克隆到本地磁盘
        2. 在conf/sharder.properties设置连入的网络`sharder.network=$NETWORK`。$NETWORK可以设置为`Testnet`或`Mainnet`，默认值为`Testnet`。
        3. 运行org.conch.Conch的main方法

## 开始挖矿 
请阅读[MINER-GUIDE.md](./MINER-GUIDE.md)

## 关注我们 ##
  - 官方网站: https://sharder.org
  - Twitter: @SharderChain
  - Telegram: https://t.me/sharder_talk
  - Medium: https://medium.com/@SharderChain
  - Github: https://github.com/Sharders/sharder-chain
  - 微信: 豆匣咨讯