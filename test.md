


          
为了提供分步实施JWT的计划，我需要先了解客户端的代码结构，因为JWT验证需要在客户端和服务端都实现。
        
            
toolName: search_codebase
            
status: success
          
query: 客户端 登录 认证 网络通信
          
# JWT功能分步实施计划

基于对代码的分析，我将提供一个分步骤实施JWT认证功能的详细计划，确保每一步都可以验证其有效性。

## 服务端实施步骤

### 步骤1：添加JWT依赖
**目标**：为项目添加JWT相关依赖

1. 修改 `vCampusServer/pom.xml`，添加以下依赖：
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

**验证方法**：运行 `mvn clean install` 确保依赖安装成功

### 步骤2：创建JWT工具类
**目标**：创建JWT令牌生成和验证的工具类

1. 创建文件 `com/example/virtual_campus/utils/JwtUtils.java`：
```java
package com.example.virtual_campus.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtils {
    private String jwtSecret = "your_jwt_secret_key"; // 实际应用中应从配置文件读取
    private int jwtExpirationMs = 86400000; // 24小时过期

    // 生成JWT令牌
    public String generateJwtToken(String username, Integer usertype) {
        return Jwts.builder()
                .setSubject(username)
                .claim("usertype", usertype)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    // 从JWT令牌获取用户名
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
    }

    // 从JWT令牌获取用户类型
    public Integer getUserTypeFromJwtToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.get("usertype", Integer.class);
    }

    // 验证JWT令牌
    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // 记录日志
            return false;
        }
    }
}
```

**验证方法**：创建测试类验证JWT生成和解析功能

### 步骤3：修改登录页面处理
**目标**：修改LoginPage在用户登录成功后生成JWT令牌

1. 修改 `com/example/virtual_campus/ServerPages/LoginPage.java` 的Login处理方法：
```java
@Autowired
private JwtUtils jwtUtils;

// 在Login case中添加JWT生成逻辑
case "Login":
    userId = (String) in.readObject();
    password = (String) in.readObject();
    System.out.println(userId);
    System.out.println(password);
    Integer LoginConfirm = authController.login(userId, password);
    
    // 登录成功后生成JWT
    if(LoginConfirm > 1) {
        Long currentUserId = authController.getUserId(userId);
        // 生成JWT令牌
        String jwtToken = jwtUtils.generateJwtToken(userId, LoginConfirm - 2); // 0=管理员, 1=教师, 2=学生
        
        // 发送结果和JWT
        out.writeObject(LoginConfirm);
        out.writeObject(currentUserId);
        out.writeObject(jwtToken);
    } else {
        out.writeObject(LoginConfirm);
    }
    out.flush();
    break;
```

**验证方法**：启动服务端，观察登录时是否生成JWT令牌

### 步骤4：创建JWT认证过滤器
**目标**：创建过滤器验证请求中的JWT令牌

1. 创建文件 `com/example/virtual_campus/security/JwtAuthenticationFilter.java`：
```java
package com.example.virtual_campus.security;

import com.example.virtual_campus.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFilter {
    @Autowired
    private JwtUtils jwtUtils;

    // 验证JWT令牌
    public boolean validateToken(String token) {
        return jwtUtils.validateJwtToken(token);
    }

    // 获取用户信息
    public String getUsernameFromToken(String token) {
        return jwtUtils.getUserNameFromJwtToken(token);
    }

    public Integer getUserTypeFromToken(String token) {
        return jwtUtils.getUserTypeFromJwtToken(token);
    }
}
```

**验证方法**：在后续步骤中集成并测试

### 步骤5：修改Server.java中handleClient方法
**目标**：在处理客户端请求时验证JWT令牌

1. 修改 `Server.java` 中的handleClient方法，添加JWT验证逻辑：
```java
@Autowired
private JwtAuthenticationFilter jwtFilter;

// 在处理客户端请求前验证JWT令牌，除登录和注册外
public void handleClient(Socket clientSocket) {
    // 现有代码...
    
    // 解析客户端请求
    String pageNumber = (String) in.readObject();
    String function = (String) in.readObject();
    
    // 对于非登录和注册的请求，验证JWT
    if (!"Login".equals(function) && !"Register".equals(function)) {
        try {
            String token = (String) in.readObject();
            if (!jwtFilter.validateToken(token)) {
                out.writeObject("401"); // 未授权
                out.flush();
                return;
            }
            // 保存当前用户信息（可选）
        } catch (Exception e) {
            out.writeObject("401"); // 未授权
            out.flush();
            return;
        }
    }
    
    // 继续处理原有业务逻辑...
}
```

**验证方法**：启动服务端，测试未携带有效JWT的请求是否被拒绝

## 客户端实施步骤

### 步骤6：创建JWT管理类
**目标**：在客户端创建管理JWT令牌的工具类

1. 创建文件 `Pages/utils/JwtManager.java`：
```java
package Pages.utils;

public class JwtManager {
    private static String jwtToken = null;

    // 保存JWT令牌
    public static void saveToken(String token) {
        jwtToken = token;
        System.out.println("JWT令牌已保存");
    }

    // 获取JWT令牌
    public static String getToken() {
        return jwtToken;
    }

    // 清除JWT令牌（登出）
    public static void clearToken() {
        jwtToken = null;
        System.out.println("JWT令牌已清除");
    }

    // 检查是否已登录
    public static boolean isLoggedIn() {
        return jwtToken != null;
    }
}
```

**验证方法**：集成到登录逻辑中并测试

### 步骤7：修改登录界面处理JWT
**目标**：在用户登录成功后保存JWT令牌

1. 修改 `LoginFrame.java` 的confirmButton方法：
```java
else if(LoginComfirm==2) {
    MainApp.setCurrentUserId((Long)in.readObject());
    // 保存JWT令牌
    String jwtToken = (String)in.readObject();
    JwtManager.saveToken(jwtToken);
    
    AdminFrame adminframe=new AdminFrame();
    adminframe.setVisible(true);
    this.dispose();
}
else if(LoginComfirm==3) {
    MainApp.setCurrentUserId((Long)in.readObject());
    // 保存JWT令牌
    String jwtToken = (String)in.readObject();
    JwtManager.saveToken(jwtToken);
    
    this.dispose();
    TeacherFrame teacherframe=new TeacherFrame();
    teacherframe.setVisible(true);
}
else if(LoginComfirm==4) {
    Long Id=(Long)in.readObject();
    // 保存JWT令牌
    String jwtToken = (String)in.readObject();
    JwtManager.saveToken(jwtToken);
    
    MainApp.setCurrentUserId(Id);
    StudentFrame studentframe=new StudentFrame();
    studentframe.setVisible(true);
    this.dispose();
}
```

**验证方法**：登录测试，检查控制台输出是否显示JWT已保存

### 步骤8：修改退出登录方法
**目标**：登出时清除JWT令牌

1. 修改 `StudentFrame.java`, `TeacherFrame.java`, `AdminFrame.java` 中的LoginOut方法：
```java
protected void LoginOut(ActionEvent ae) {
    if(JOptionPane.showConfirmDialog(this, "是否确认退出?","正在退出...",JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
        // 清除JWT令牌
        JwtManager.clearToken();
        
        LoginFrame loginframe=new LoginFrame();
        loginframe.setVisible(true);
        this.dispose();
    }
}
```

**验证方法**：测试登出功能，检查控制台输出是否显示JWT已清除

### 步骤9：修改客户端网络请求添加JWT
**目标**：所有非登录/注册的网络请求都添加JWT令牌

1. 创建一个包装方法在MainApp中：
```java
public static void sendRequestWithToken(String pageNumber, String function) throws IOException {
    out.writeObject(pageNumber);
    out.writeObject(function);
    // 如果不是登录或注册，添加JWT令牌
    if (!"Login".equals(function) && !"Register".equals(function)) {
        out.writeObject(JwtManager.getToken());
    }
}
```

2. 修改ChangePasswordFrame中的ComFirmButton方法：
```java
out.writeObject("1");
out.writeObject("ChangePassword");
// 添加JWT令牌
out.writeObject(JwtManager.getToken());
out.writeObject(MainApp.getCurrentUserId());
```

**验证方法**：测试各功能模块，确认请求包含JWT令牌

## 集成验证步骤

### 步骤10：端到端测试
1. 启动服务端
2. 启动客户端并登录
3. 验证各功能模块是否正常工作
4. 测试token过期和无效token的处理
5. 测试登出后再访问受限资源的情况

**验证方法**：监控服务端和客户端日志，检查JWT验证是否正常工作

## 每步验证标准
- **步骤1**：依赖成功安装，无编译错误
- **步骤2**：JWT工具类能够正确生成和验证令牌
- **步骤3**：服务端登录响应包含JWT令牌
- **步骤4**：过滤器能正确验证令牌的有效性
- **步骤5**：未携带有效JWT的请求被拒绝
- **步骤6**：JWT管理类能正确保存和获取令牌
- **步骤7**：客户端登录后能正确保存JWT令牌
- **步骤8**：登出后JWT令牌被正确清除
- **步骤9**：请求中包含JWT令牌
- **步骤10**：端到端功能正常，认证机制有效

通过以上分步实施计划，您可以逐步添加JWT功能，每一步都可以独立验证，确保整个流程的正确性。
        