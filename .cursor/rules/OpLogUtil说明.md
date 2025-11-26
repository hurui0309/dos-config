# OpLogUtil 使用手册

## 概述
`cn.webank.weup.base.log.OpLogUtil` 是一个weup框架提供用于记录操作日志的工具类
它提供了一系列静态方法来记录不同类型的日志，例如普通操作步骤、异常情况、警告、错误和致命错误等。

## 主要功能

1. **记录普通操作步骤**: `logOpStep`
2. **记录异常情况**: `logOpStepException`
3. **记录警告信息**: `onWarning`
4. **记录错误信息**: `onError`
5. **记录致命错误信息**: `onFatal`
6. **根据日志级别记录信息**: `logOpWithLevel`

## 方法详解

### 1. 记录普通操作步骤

```java
public static void logOpStep(String actionName, String actionResult, Object... extInfos)
public static void logOpStep(OpLogActionAble<? extends Enum> actionEnum, String actionResult, Object... extInfos)
```

- `actionName`: 操作名，不能为空，不能含有竖线。
- `actionEnum`: 枚举形式的op操作名。
- `actionResult`: 操作结果或当前阶段信息，不能为空，不能含有竖线。
- `extInfos`: 操作附加信息，不定长参数。

### 2. 记录异常情况

```java
public static void logOpStepException(String actionName, String actionResult, Throwable t, Object... extInfos)
public static void logOpStepException(OpLogActionAble<? extends Enum> actionEnum, String actionResult, Throwable t, Object... extInfos)
```

- `actionName`: 操作名，不能含有竖线。
- `actionEnum`: 枚举形式的op操作名。
- `actionResult`: 操作结果或当前阶段信息，不能含有竖线。
- `t`: 出错异常。
- `extInfos`: 操作附加信息，不定长参数。

### 3. 记录警告信息

```java
public static void onWarning(String actionName, String reason, Object... extInfos)
public static void onWarning(OpLogActionAble<? extends Enum> actionEnum, String reason, Object... extInfos)
```

- `actionName`: 操作名，不能为空，不能含有竖线。
- `actionEnum`: 枚举形式的op操作名。
- `reason`: 操作结果或当前阶段信息，不能为空，不能含有竖线。
- `extInfos`: 操作附加信息，不定长参数。

### 4. 记录错误信息

```java
public static void onError(String actionName, String reason, Object... extInfos)
public static void onError(OpLogActionAble<? extends Enum<?>> actionEnum, String reason, Object... extInfos)
```

- `actionName`: 操作名，不能为空，不能含有竖线。
- `actionEnum`: 枚举形式的op操作名。
- `reason`: 操作结果或当前阶段信息，不能为空，不能含有竖线。
- `extInfos`: 操作附加信息，不定长参数。

### 5. 记录致命错误信息

```java
public static void onFatal(String actionName, String reason, Object... extInfos)
public static void onFatal(OpLogActionAble<? extends Enum> actionEnum, String reason, Object... extInfos)
```

- `actionName`: 操作名，不能为空，不能含有竖线。
- `actionEnum`: 枚举形式的op操作名。
- `reason`: 操作结果或当前阶段信息，不能为空，不能含有竖线。
- `extInfos`: 操作附加信息，不定长参数。

### 6. 根据日志级别记录信息

```java
public static void logOpWithLevel(WeupOpLogLevel level, String actionName, String reason, Object... extInfos)
public static void logOpWithLevel(WeupOpLogLevel level, OpLogActionAble<? extends Enum> actionEnum, String reason, Object... extInfos)
```
- `level`: 日志级别。
- `actionName`: 操作名，不能为空，不能含有竖线。
- `actionEnum`: 枚举形式的op操作名。
- `reason`: 操作结果或当前阶段信息，不能为空，不能含有竖线。
- `extInfos`: 操作附加信息，不定长参数。


## 使用示例

### 示例1: 记录普通操作步骤

```java
import cn.webank.weup.base.log.OpLogUtil;

public class ExampleService {
    public void doSomething() {
        // 记录操作开始
        OpLogUtil.logOpStep("ExampleService.doSomething", "start");
        
        // 执行业务逻辑
        // ...
        
        // 记录操作结束
        OpLogUtil.logOpStep("ExampleService.doSomething", "end", "result=success");
    }
}
```

### 示例2: 记录异常情况

```java
import cn.webank.weup.base.log.OpLogUtil;

public class ExampleService {
    public void doSomething() {
        try {
            // 执行可能出错的业务逻辑
            otherMethod();
        } catch (Exception e) {
            // 记录异常情况
            OpLogUtil.logOpStepException("ExampleService.doSomething", "failed", e, "operation=riskyOperation");
        }
    }

}
```

### 示例3: 使用枚举形式的操作名

- 首先定义枚举类：

```java
import cn.webank.weup.base.log.OpLogActionAble;

public enum BusinessOpAction implements OpLogActionAble<BusinessOpAction> {
    USER_LOGIN,
    USER_LOGOUT,
    DATA_PROCESSING;
}
```

- 然后在代码中使用BusinessOpAction类：

```java
import cn.webank.weup.base.log.OpLogUtil;
import static com.example.BusinessOpAction.*;

public class BusinessService {
    public void userLogin(String userId) {
        // 记录用户登录操作
        OpLogUtil.logOpStep(USER_LOGIN, "success", "userId=" + userId);
    }
    
    public void processData() {
        try {
            // 记录数据处理开始
            OpLogUtil.logOpStep(DATA_PROCESSING, "start");
            
            // 执行数据处理逻辑
            // ...
            
            // 记录数据处理结束
            OpLogUtil.logOpStep(DATA_PROCESSING, "end", "records=100");
        } catch (Exception e) {
            // 记录数据处理异常
            OpLogUtil.logOpStepException(DATA_PROCESSING, "failed", e);
        }
    }
}
```

### 示例4: 记录不同级别的日志

```java
import cn.webank.weup.base.log.OpLogUtil;
import cn.webank.weup.base.log.WeupOpLogLevel;

public class LogLevelExample {
    public void demonstrateLogLevels() {
        // 记录INFO级别日志
        OpLogUtil.logOpWithLevel(WeupOpLogLevel.INFO, "LogLevelExample.demo", "info message");
        
        // 记录WARNING级别日志
        OpLogUtil.onWarning("LogLevelExample.demo", "warning message");
        
        // 记录ERROR级别日志
        OpLogUtil.onError("LogLevelExample.demo", "error message");
        
        // 记录FATAL级别日志
        OpLogUtil.onFatal("LogLevelExample.demo", "fatal message");
    }
}
```

### 示例5: 使用扩展信息

```java
import cn.webank.weup.base.log.OpLogUtil;

public class ExtInfoExample {
    public void demonstrateExtInfo() {
        String userId = "12345";
        String orderId = "ORD-67890";
        String[] items = {"item1", "item2", "item3"};
        
        // 记录带有多项扩展信息的日志
        OpLogUtil.logOpStep("ExtInfoExample.processOrder", "completed", 
                           "userId=" + userId, 
                           "orderId=" + orderId, 
                           "itemCount=" + items.length, 
                           items);
    }
}
```