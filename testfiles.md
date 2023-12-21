# 测试文件说明
## test1.txt
基础的功能测试。使用了int、float、string等基本类型。
程序输出：
```
Hello, World!
RPCL ver 1.01
Input an integer to get Fibonacci
15
fib is 610
```
## test2.txt
基础的容器功能测试。

程序输出：
```
Please give me a number!
2306122
two two one six null three two 
Have a nice Day!
```
## test3.txt
基础并行原语能力测试。
输出：
```
ping!
pong!
ping!
pong!
ping!
pong!
ping!
pong!
ping!
pong!
ping!
Package loss rate 0
pong!
TTL 0s
```

其他未测试的重要特性：
- shared list/dict 支持并发读写
- 比较运算符支持字符串字典序比较
- 列表/字典为空时布尔值为false
- 计算时自动int->float, 复制时自动转型

