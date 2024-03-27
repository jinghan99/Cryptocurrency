项目导入后，要设置maven环境。
Maven运行环境在环境安装脚本执行时已准备好，在 C:\northstar-env目录下。

导入northstar-external外置策略项目
   首先要确保对 northstar 项目执行过 mvn install
   mvn安装

然后便可以根据模板创建一个 northstar-external 项目 创建项目

第一个红框：填项目名称，northstar-external
第二个红框：选择模板项目及模板项目版本，如果没有archetype，在确保已经执行过 mvn install 的前提下，可添加对应的archetype文件（在你的.m2/repository目录下）
第三个红框：填 northstar-external 项目的GAV坐标：
ArtifactId必须是 northstar-external
Version要与 Northtsar 主项目一致
创建好后，检查新项目的 pom.xml
如果发现像下图情况，${northstarVersion} 这个变量没有被成功赋值，请手动替换成具体的主项目版本号 pom文件检查

手动修改版本号后，需要 reload 一下项目才会生效 IDEA刷新项目

#3. 设置以上两项目的依赖关系
到此为止，northstar 与 northstar-external 项目还是相互独立的两个项目。还需要一些设置来建立依赖关系。 IDEA项目依赖设置

TIP

注意：northstar-external 要设置为 northstar-main 的 dependencies，scope为 runtime

### 添加services springboot 显示端口

```angular2html
 <component name="RunDashboard">
  <option name="configurationTypes">
    <set>
      <option value="SpringBootApplicationConfigurationType" />
    </set>
  </option>
</component>
```
```shell
java  -jar "-Dloader.path=$(pwd)"  "-Dhttp.proxyHost=127.0.0.1" "-Dhttp.proxyPort=10810" "-Dhttps.proxyHost=127.0.0.1" "-Dhttps.proxyPort=10810" "-Dfile.encoding=UTF-8"   -Denv=dev northstar-7.1.0.jar  
```