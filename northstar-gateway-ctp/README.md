# northstar-gateway-ctp

northstar盈富量化平台CTP网关接口实现

## 如何使用
使用时，需要依赖 northstar 主程序进行加载，**把 northstar-gateway-ctp-<版本号>.jar 与主程序jar包置于同一目录下，然后启动主程序**。


## 如何选择其他期货公司
![输入图片说明](https://foruda.gitee.com/images/1695827288572876856/57dbd874_1676852.png "屏幕截图")

项目已经提供了ctp-channels.json定义文件，把它放置于与ctp网关jar同一目录下即可。默认提供了【宏源】期货的连接渠道。如需增加，可自行按样例扩展json文件。  

在开发时，也可以通过提供环境变量NS_CTP_CHANNEL_FILE来指定json文件路径。


## 注意事项
**必须校准服务器时间，否则会接收不到行情**