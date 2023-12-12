# 更新日志
## 加入并发
```angular2html
//带有thread的函数表示可以作为一个线程独立执行
FuncDef -> ['thread'] 'def' Ident '('[FormParams]')' ['=>' Type] Block

//只能在主函数中调用线程
CallThreadStmt -> 'run' Ident ';'

//共享变量，加入'shared'关键字，只能在全局定义，举例int shared x
TType -> 'shared'

//关于锁
LockStmt -> 'lock' Ident ';'
UnlockStmt -> 'unlock' Ident ';'
```