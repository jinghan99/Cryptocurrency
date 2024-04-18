#!/bin/bash

# 设置jar文件名
# 查找/root/northstar-dist目录下符合命名规则的jar文件，并将其路径赋值给变量APP_NAME
APP_NAME=$(find /root/northstar-dist -name 'northstar-[0-9]*.*.jar')

# 当前日期
# 获取当前日期，并将其格式化为YYYY-MM-DD的形式赋值给变量CURRENT_DATE
CURRENT_DATE=$(date +"%Y-%m-%d")

# 日志文件名
# 指定日志文件的路径和文件名为/root/northstar-dist/ns.log，赋值给变量LOG_FILE
LOG_FILE="/root/northstar-dist/logs/Northstar_${CURRENT_DATE}.log"

#使用说明，用来提示输入参数
# 定义函数usage，用于显示脚本的使用说明
usage() {
    echo "Usage: sh 执行脚本.sh [start|stop|restart|status|log]"
    exit 1
}

#检查程序是否在运行
# 定义函数is_exist，用于检查指定的程序是否在运行
is_exist() {
    # 获取指定程序的进程ID，并将结果赋值给变量pid
    pid=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
    # 如果pid为空，则表示程序未运行，返回1；否则返回0
    if [ -z "${pid}" ]; then
        return 1
    else
        return 0
    fi
}

#启动方法
# 定义函数start，用于启动指定的程序
start() {
    # 调用is_exist函数检查程序是否在运行
    is_exist
    # 如果程序已经在运行，则输出提示信息，并显示程序的进程ID
    if [ $? -eq "0" ]; then
        echo "${APP_NAME} is already running. pid=${pid} ."
    else
        # 如果程序未运行，则使用nohup命令启动程序，并将输出重定向到/dev/null，使其在后台运行
        nohup java -Duser.timezone=Asia/Shanghai -Xms512m -Xmx512m -jar $APP_NAME --spring.profiles.active=prod >/dev/null 2>&1 &
        # 获取新启动进程的pid并输出
        pid=$!
        echo "Starting ${APP_NAME} with pid=${pid} ."
    fi
}

#停止方法
# 定义函数stop，用于停止指定的程序
stop() {
    # 调用is_exist函数检查程序是否在运行
    is_exist
    # 如果程序在运行，则使用kill命令终止其进程；否则输出提示信息
    if [ $? -eq "0" ]; then
        kill -9 $pid
    else
        echo "${APP_NAME} is not running"
    fi
}

#输出运行状态
# 定义函数status，用于显示指定程序的运行状态
status() {
    # 调用is_exist函数检查程序是否在运行
    is_exist
    # 如果程序在运行，则输出提示信息，并显示程序的进程ID；否则输出提示信息表示程序未运行
    if [ $? -eq "0" ]; then
        echo "${APP_NAME} is running. Pid is ${pid}"
    else
        echo "${APP_NAME} is NOT running."
    fi
}

# 打印日志最后100行
# 定义函数log，用于显示日志文件的最后100行内容
log() {
    tail -f -n 100 "$LOG_FILE"
}

#重启
# 定义函数restart，用于重启指定的程序
restart() {
    # 调用stop函数停止程序
    stop
    # 调用start函数启动程序
    start
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
# 使用case语句根据用户输入的参数选择执行对应的方法，如果参数不在指定的范围内，则执行使用说明
case "$1" in
    "start")
        start
        ;;
    "log")
        log
        ;;
    "stop")
        stop
        ;;
    "status")
        status
        ;;
    "restart")
        restart
        ;;
    *)
        usage
        ;;
esac
