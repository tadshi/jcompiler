# 语言问题记录

## 常量初始化
由于对常量数组初始化列表进行了粗暴的处理，因此无论是任何形式的初始化列表，只要元素数量与数组定义一致，都能初始化数组。

而由于List和数组共用一套处理代码，因此初始化List时会直接展开所有的初始化列表。

改正的话需要逐层检查。由于时间较少并且不甚紧急，因此暂且搁置。

e.g.
```
List<int> list = {1,{2,{3}}};
```
最后会产生一个`[1,2,3]`的列表。

## 数组维数限制
数组每一维最多为65535，因为我用了SIPUSH。

其实很好改，但是……也没什么必要？

## 多维数组支持
理论上后端能够支持多维数组，但是不支持高维数组转成低维数组。这是因为后端并没有为数组新建一套类型系统，所有数组的类型都是其内容。

修改需要向类型系统中加入数组。工作量很大。

