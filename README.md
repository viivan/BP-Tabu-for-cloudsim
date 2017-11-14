# BP-Tabu
一种基于改进禁忌搜索的云任务负载均衡调度策略。该策略综合考虑任务总完成时间及虚拟机负载均衡度，提出时间贪心初始解求解步骤，进而引入结合多因素优值函数的Tabu算法优化任务调度的负载均衡，再进一步给出跳出局部最优的惩戒策略。

# DatacenterBroker.java
DataCenterBroker为数据中心代理的功能实现类

# TabuExample.java
TabuExample为算法测试类

# cloudsim 下载
本项目使用cloudsim-3.0.3.zip
```
https://github.com/Cloudslab/cloudsim/releases
```
# 使用
1.  解压cloudsim-3.0.3.zip
2.  将项目引入ide中
3.  在examples文件夹下添加本项目中的类
4.  运行TabuExample.java