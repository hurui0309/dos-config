# 基于weup框架的RMB服务开发说明
- 基于weup框架开发RMB服务，通常涉及Handler类，Req类和Rsp类的开发
- weup框架收到RMB请求，使用Handler类(RmbRequestHandler接口子类)的handle()方法处理具体请求逻辑
- weup框架使用Req类和Rsp类，分别承载服务请求和响应内容

# 总体要求
- Req和Rsp类定义总体上遵循 Java POJO 格式且不要实现 Serializable 接口（报文交换格式为 JSON，由 jackson 组件序列化与反序列化）
- 编写RMB服务代码前，可以先在代码中挑选2个已有服务的Handler，Req和Rsp类，通过读取存量代码实现，了解并遵循现有代码习惯，比如package结构，公共父类，命名等
- Handler类实现通常会继承一个公共父类，请确认存量Handler代码是否有公共父类，新Handler代码需要和存量保持一致

# Handler类说明

## 命名规范
- Handler实现类的类名命名惯例：RMBReq_{serviceId}_{scenarioId}Handler
- 子命令对应的Handler类名命名管理：RMBReq_{serviceId}_{scenarioId}_{subCommand}Handler

## 日志规范

## 防止重复请求
如果服务类型为非查询类，要求实现 RMB/MASA 重复消息检测和拦截。如果没有启用 weup 框架的主动防重机制，可以通过数据库记录主键冲突/捕获后抛出 WeupRmbRepeatException 来实现。

## 联机转异步
如果接口是耗时很长（大于 RMB/MASA 调用超时），需要设计成异步通知模式。联机请求入库后通过 WeupRmbUtil 的 asyncRunWithContext() 或 asyncCallWithContext() 转异步线程处理，并返回收妥应答。注意异步线程池不能与当前线程池相同。

## Handler代码实现样例
```java
/**
 * xxx服务处理器
 *
 * @author 作者名
 */
@Service
public class RMBReq_04500240_01Handler implements RmbRequestHandler<Req_04500240_01, Rsp_04500240_01> {

    @Override
    public Rsp_04500240_01 handle(Req_04500240_01 req, WeupRmbAppHeader weupRmbAppHeader) throws WeupRmbBizException, WeupRmbSysException {
        Rsp_04500240_01 resp = new Rsp_04500240_01();
        try {
            // 可选打印收到请求日志信息
            OpLogUtil.logOpStep("接收服务: 04500240", "begin", JSONUtil.toDenseJsonStr(req));
            // 补充具体请求处理逻辑。。。            
        } catch (Exception e) {
            // 打印OnError日志，用于告警
            OpLogUtil.onError("接收服务: 04500240", "服务处理异常，检查具体日志内容定位原因", JSONUtil.toDenseJsonStr(req), e);
        }

        return resp;
    }
}
```

# Req类和Rsp类说明
## 类定义
- Req命名规范：Req_{serviceId}_{scenarioId}
- Rsp命名规范：Rsp_{serviceId}_{scenarioId}
- Req类必须加上 @RmbMessage 注解，属性包括：
  * name: 接口名称
  * serviceId: 服务ID
  * scenarioId: 场景ID(默认"01")
  * useDesc: 服务详细描述，主要包含调用说明，实现逻辑，SLA约束，注意事项等内容
  * threadPoolName: 指定执行RMB请求的线程池的bean，可省略使用默认线程池

## 子命令
- weup框架支持定义RMB子命令，不同子命令使用不同的Req和Rsp类
- 子命令需要使用Req_{serviceId}_{scenarioId}定义所有子命令请求体公共父类，里面包含了所有子命令公共字段
- 请求体父类需要有一个@RmbFieldType(RmbFieldType.Type.SUB_COMMAND)注解标注的字段用来区分不同子命令
- 子命令Req对象命令规范：Req_{serviceId}_{scenarioId}_{subCommand}，需要继承Req_{serviceId}_{scenarioId}
- 子命令Rsp对象命令规范：Rsp_{serviceId}_{scenarioId}_{subCommand}，可以选择继承Rsp_{serviceId}_{scenarioId}

## 限流
- 提供的服务都要考虑合适限流，预防服务器被打爆
- weup在Req类的任意字段上, 加上RmbReqFrequencyLimit注解来做限流，样例如下

```java 
public class Req_03301346_01 {
    // 限流10次请求每秒
    @RmbReqFrequencyLimit(maxCount = 10, periodMillis = 1000)
    @JsonIgnore
    private String limit = "limit";

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }
}
```

## 字段注解
- 多余操作类服务，Req类每个字段须考虑加校验注解（@WeupNotBlank, @WeupNotNull 等），对于查询类服务，正常无需添加字段校验注解
- 添加 @RmbField 注解说明字段名，字段用途，和字段序号
- 当协议中字段名与成员变量名不一致时用 @JsonProperty 进行转换

## 字段命名
- 字段命名遵循业务系统的字典定义，避免同一概念名称不同
- boolean 类型的变量名不能用 is 开头，因为不同的序列化/反序列化组件的处理存在差异
- 禁止使用首字母小写次字母大写的命名字段，如 iPhone，因为不同的序列化/反序列化组件的处理存在差异
- 字段名要和服务涉及的数据库相关表列名保持一致
- 直接定义或使用 @JsonProperty 转换后的字段名禁止出现且不限于以下现象：
  * 忽略大小写、分隔符后出现同名字段，如 cdnNo、cdn_no、CDNNO
  * 协议子类定义与父类同名的字段
  * 协议报文体与报文头同名字段，如流水号

## 字段类型
- 字段类型与功能严格对应，避免类型转换导致的问题，例如用字符串传输数值。其次，数据的格式、参照等信息必须明确给出。例如：
  * 用 Long 传输时间戳必须给出参考点（如 UTC 0）
  * 用字符串传输日期、时间，必须给出 format 的格式。
- 浮点数字段类型必须使用 BigDecimal，禁止使用 IEEE 754 无法精确表达浮点数的 float、double 类型。
- List、数组等集合类型字段禁止 null 值
  * 这类字段必须明确标上 **@WeupNotNull** 注解，没有元素时传 empty 对象，而不是 null 值。

## 格式要求
  * 字段间用空行分隔
  * getter/setter 统一放在类末尾

# 示例
## 请求类示例
```java
import cn.webank.weup.rmb.annotation.RmbField;
import cn.webank.weup.rmb.annotation.RmbMessage;
import cn.webank.weup.validate.annotations.WeupNotNull;
import cn.webank.weup.validate.annotations.strings.TextLengthRange;
import cn.webank.weup.validate.annotations.strings.WeupNotBlank;

@RmbMessage(serviceId = "12345678", scenarioId = "01", name = "上报用户服务", 
        useDesc = "用于上报用户名", threadPoolName = "Tp_Main")
public class Req_12345678_01 {
    @WeupNotBlank
    @TextLengthRange(minLen = 3, maxLen = 32)
    @RmbField(seq = 1, title = "user", remark = "用户")
    private String user;

    public String getUser() {
        return user;
    }

  public void setUser(String user) {
    this.user = user;
  }
}
```
### 子命令请求类示例
```java
import cn.webank.weup.rmb.annotation.RmbField;
import cn.webank.weup.rmb.annotation.RmbMessage;
import cn.webank.weup.validate.annotations.WeupNotNull;
import cn.webank.weup.validate.annotations.strings.TextLengthRange;
import cn.webank.weup.validate.annotations.strings.WeupNotBlank;

@RmbMessage(serviceId = "12345678", scenarioId = "01", name = "上报用户年龄服务", 
        useDesc = "用于上报年龄子命令", threadPoolName = "Tp_Main")
public class Req_12345678_01_Age extends Req_12345678_01 {
    @WeupNotNull
    @RmbField(title = "age", remark = "用户")
    private Integer age;

    public Integer getAge() {
        return age;
    }

  public void setAge(Integer age) {
    this.age = age;
  }
}
```

## 应答类示例
```java
import cn.webank.weup.rmb.annotation.RmbField;
import cn.webank.weup.rmb.annotation.RmbMessage;

@RmbMessage(serviceId = "12345678", scenarioId = "01")
public class Rsp_12345678_01 {
    @RmbField(title = "处理结果", remark = "描述请求处理结果")
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
```
### 子命令相应类示例
```java
import cn.webank.weup.rmb.annotation.RmbField;
import cn.webank.weup.rmb.annotation.RmbMessage;
import cn.webank.weup.validate.annotations.WeupNotNull;
import cn.webank.weup.validate.annotations.strings.TextLengthRange;
import cn.webank.weup.validate.annotations.strings.WeupNotBlank;

@RmbMessage(serviceId = "12345678", scenarioId = "01")
public class Rsp_12345678_01_Age extends Rsp_12345678_01 {
  @RmbField(title = "处理结果", remark = "描述请求处理结果")
  private String result;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }
}
```

# 使用weup框架调用RMB服务说明

## 请求参数构造
调用RMB服务，参考 cn.webank.weup.biz.rmb.WeupRmbUtil 的调用方法：

```java
// 调用RMB的同步Q
public static <T> T rmbCall(Object rmbRequestObject, String targetDCN, String bizSeq, String sysSeq, WeupRmbAppHeader weupRmbAppHeader)

// 调用RMB的异步Q
public static void rmbAsyncCall(Object rmbRequestObject, String targetDCN, String bizSeq, String sysSeq, WeupRmbAppHeader weupRmbAppHeader)
```

- 请求类（rmbRequestObject）定义上增加 @EnableProdValidate 注解并设置属性 enableOnSendRmbRequest = true，weup 框架会自动触发协议参数校验，否则此校验只在测试环境生效。
- 根据服务提供者部署的 DCN 以及服务治理中 ServiceID 的属性，合理设置被调用服务所在 DCN 信息（targetDCN）参数
- 按 RMB 调用基本法，请求接口中的业务流水号（bizSeq）必须与当前交易上游传入的bizSeq一致
- 按 RMB 调用基本法，请求接口中的系统调用（sysSeq）必须用WeupRmbUtil.genSysSeqLen32()新建唯一的流水号

## 调用过程的注意事项
- 根据调用服务 ServiceID 的“服务性质”，对非查询类要先记录请求信息和状态到数据库，再发起服务调用
- RMB 服务调用不能处于数据库事务代码中，防止阻塞事务
- 如果服务调用处于循环逻辑中（例如批量任务），需要控制调用的节奏，避免触发下游流量控制
- 调用发生在定时任务、异步线程时，需要注意 WeupRmbAppHeader 内容的设置与清理

## 异常捕获
通过 weup 接口调用 RMB 需要显式捕获以下三类 checked exception：
- WeupRmbCallTimeOutException 超时异常
- WeupRmbBizException 业务类异常
- WeupRmbSysException 系统异常
如果是查询类服务，则可以只捕获父类异常 WeupRmbCallException。

## 告警日志
调用出现异常需要在捕获异常的代码中输出告警日志。

## 异常续做
非查询类服务，超时异常和系统异常不更新状态，由续做逻辑处理，可以借助 weup-txn-resume 组件简化续做代码逻辑。
续做时要根据服务接口是否幂等、是否提供查询来决定合理的续做延时和节奏。