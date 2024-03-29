# 更新日志
## 加入并发
```angular2html
//带有thread的函数表示可以作为一个线程独立执行
FuncDef -> ['thread'] 'def' Ident '('[FormParams]')' ['=>' Type] Block

//只能在主函数中调用线程
CallThreadStmt -> 'run' funcCall ';'

//共享变量，前面加入'shared'关键字，只能在全局定义,int shared x的形式
ParallelModifier -> 'shared'
VarDecl -> [ParallelModifier] Type VarDef {, VarDef}';'

//并发原语
ParallelType -> 'lock' | 'semaphore'
ParallelDecl -> ParallelType VarDef {, VarDef}';'

//关于锁
AwaitStmt -> 'await' Ident | funcCall ';'
SignalStmt -> 'signal' Ident';'
```
## 加入其他类型
```angular2html
Type -> 'int' | 'float' | 'bool' | 'string' | 'list''<'Type'>' | 'dict''<'Type',Type'>'
Number ::= [+|-][0-9]+
Float ::= [+|-][0-9]+'.'[0-9]+
Bool ::= 'true' | 'false'

String ::= '"' characters '"'
characters ::= character*
character ::= letter | digit | special-char
letter ::= 'a' | 'b' | ... | 'z' | 'A' | 'B' | ... | 'Z'
digit ::= '0' | '1' | ... | '9'
special-char ::= any printable ASCII character except '"' (double quote)

InitList ::= '['{litem}{',' litem}']'
litem ::= int | float | bool | string  (same type)

//列表放入值，一次只能放入一个 列表a.add(1)
PutItemStmt ::= Ident'.''add''('Exp')'';'

InitDict ::= '{'{pair}{','pair}'}'
pair ::= key ':' value
key ::= int | float | string
value ::= int | float | bool | string  (keys are the same type, so as values)


```
## 函数定义和函数闭包
```angular2html
// 函数闭包 处理成了FuncDef的形式 无需后端额外处理
FuncDecl -> 'fn' Ident '=' FuncClosure ';'
FunClosure -> 'def''('FormParams')'['=>' Type] Block

//定义函数  
FuncDef -> 'def' Ident '('[FormParams]')' ['=>' Type] Block
FormParams -> FormParam {',' FormParam}
// todo：定义参数类型（[默认参数]）和返回值类型？
FormParam -> Type Ident ['[' ']' { '[' ConstExp ']' }]
```