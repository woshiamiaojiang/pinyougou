# DAY04

# 2.运营商系统登录与安全控制

## Spring Security安全框架

修改pinyougou-manager-web的pom.xml ，添加依赖

```xml
<!-- 身份验证 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-config</artifactId>
</dependency>
```

修改web.xml 

```xml
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>classpath:spring/spring-security.xml</param-value>
</context-param>
<listener>
    <listener-class>
        org.springframework.web.context.ContextLoaderListener
    </listener-class>
</listener>

<filter>
    <filter-name>springSecurityFilterChain</filter-name>
    <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
</filter>
<filter-mapping>
    <filter-name>springSecurityFilterChain</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

pinyougou-manager-web的spring目录下添加配置文件spring-security.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns="http://www.springframework.org/schema/security"
   xmlns:beans="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                  http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security.xsd">
   
   <!-- 设置页面不登陆也可以访问 -->
   <http pattern="/*.html" security="none"></http>
   <http pattern="/css/**" security="none"></http>
   <http pattern="/img/**" security="none"></http>
   <http pattern="/js/**" security="none"></http>
   <http pattern="/plugins/**" security="none"></http>

   <!-- 页面的拦截规则    use-expressions:是否启动SPEL表达式 默认是true -->
   <http use-expressions="false">
      <!-- 当前用户必须有ROLE_USER的角色 才可以访问根目录及所属子目录的资源 -->
      <intercept-url pattern="/**" access="ROLE_ADMIN"/>
      <!-- 开启表单登陆功能 -->
      <form-login  login-page="/login.html" default-target-url="/admin/index.html" authentication-failure-url="/login.html" always-use-default-target="true"/>
      <csrf disabled="true"/>
      <headers>
         <frame-options policy="SAMEORIGIN"/>
      </headers>
      <logout/>
   </http>
   
   <!-- 认证管理器 -->
   <authentication-manager>
      <authentication-provider>
         <user-service>
            <user name="admin" password="123456" authorities="ROLE_ADMIN"/>
            <user name="sunwukong" password="dasheng" authorities="ROLE_ADMIN"/>
         </user-service>
      </authentication-provider> 
   </authentication-manager>
      
</beans:beans>
```

修改pinyougou-manager-web的 login.html 

```html
<form class="sui-form" action="/login" method="post" id="loginform">

   <div class="input-prepend"><span class="add-on loginname"></span>
      <input id="prependedInput" name="username" type="text" placeholder="邮箱/用户名/手机号" class="span2 input-xfat">
   </div>
   <div class="input-prepend"><span class="add-on loginpwd"></span>
      <input id="prependedInput" name="password" type="password" placeholder="请输入密码" class="span2 input-xfat">
   </div>
   <div class="setting">
       <div id="slider">
         <div id="slider_bg"></div>
         <span id="label">>></span> <span id="labelTip">拖动滑块验证</span>
         </div>
   </div>
   <div class="logined">

      <a class="sui-btn btn-block btn-xlarge btn-danger" onclick="document:loginform.submit()" target="_blank">登&nbsp;&nbsp;录</a>
   </div>

</form>

```

测试

![1547026684227](assets/1547026684227.png)

![1547026695699](assets/1547026695699.png)

## 主界面显示登陆人

在pinyougou-manager-web新建LoginController.java

```java
package com.pinyougou.manager.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {
    @RequestMapping("name")
    public Map name() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        Map map = new HashMap();
        map.put("loginName", name);
        return map;
    }
}
```

（1）新建loginService.js

```javascript
//登陆服务层
app.service('loginService',function($http){
    //读取登录人名称
    this.loginName=function(){
        return $http.get('../login/name.do');
    }
});
```

（2）新建indexController.js

```javascript
app.controller('indexController' ,function($scope,$controller   ,loginService){
    //读取当前登录人
    $scope.showLoginName=function(){
        loginService.loginName().success(
            function(response){
                $scope.loginName=response.loginName;
            }
        );
    }
});
```

修改index.html

```html
<script type="text/javascript" src="../plugins/angularjs/angular.min.js"></script>
<script type="text/javascript" src="../js/base.js"></script>
<script type="text/javascript" src="../js/service/loginService.js"></script>
<script type="text/javascript" src="../js/controller/indexController.js"></script>

<body class="hold-transition skin-green sidebar-mini" ng-app="pinyougou" ng-controller="indexController" ng-init="showLoginName ()">>


<a href="#" class="dropdown-toggle" data-toggle="dropdown">
    <img src="../img/user2-160x160.jpg" class="user-image" alt="User Image">
    <span class="hidden-xs">{{loginName}}</span>
</a>
<p>
    {{loginName}}
    <small>最后登录 11:20AM</small>
</p>
<div class="pull-left info">
    <p> {{loginName}}</p>
    <a href="#"><i class="fa fa-circle text-success"></i> 在线</a>
</div>

```

测试

![1547027479510](assets/1547027479510.png)

## 退出登录

spring-security.xml

```xml
<logout/>
```

index.html

```html
<div class="pull-right">
    <a href="../logout" class="btn btn-default btn-flat">注销</a>
</div>
```

测试

![1547027765188](assets/1547027765188.png)

![1547027772904](assets/1547027772904.png)

# 3.商家申请入驻

## 3.1需求分析

商家申请入驻，需要填写商家相关的信息。待运营商平台审核通过后即可使用使用。

## 3.2准备工作

（1）拷贝资源： 将“资源/静态原型/商家管理后台”下的页面拷贝到pinyougou-shop-web工程

![img](assets/clip_image002.jpg)

![img](assets/clip_image004.jpg)

（2）参照“运营商后台”构建js 

![img](assets/clip_image006.jpg)

（3）拷贝后端控制层代码

![img](file:///C:/Users/miion/AppData/Local/Temp/msohtmlclip1/01/clip_image008.jpg)

## 3.3前端代码

修改register.html 引入JS

```html
<script type="text/javascript" src="plugins/angularjs/angular.min.js">  </script>
<script type="text/javascript" src="js/base.js">  </script>
<script type="text/javascript" src="js/service/sellerService.js">  </script>
<script type="text/javascript" src="js/controller/baseController.js">  </script>
<script type="text/javascript" src="js/controller/sellerController.js">  </script>
```

指令

```html
<body ng-app="pinyougou" ng-controller="sellerController">
```

绑定表单
```html
<div class="control-group">
   <label class="control-label">登陆名（不可修改）：</label>
   <div class="controls">
      <input type="text" placeholder="登陆名" ng-model="entity.sellerId" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">登陆密码：</label>
   <div class="controls">
      <input type="password" placeholder="登陆密码" ng-model="entity.password" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">店铺名称：</label>
   <div class="controls">
      <input type="text" placeholder="店铺名称" ng-model="entity.nickName" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">公司名称：</label>
   <div class="controls">
      <input type="text" placeholder="公司名称" ng-model="entity.name" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">公司电话：</label>
   <div class="controls">
      <input type="text" placeholder="公司电话" ng-model="entity.telephone" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">公司详细地址：</label>
   <div class="controls">
      <input type="text" placeholder="公司详细地址" ng-model="entity.addressDetail" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">联系人姓名：</label>
   <div class="controls">
      <input type="text" placeholder="联系人姓名" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">联系人QQ：</label>
   <div class="controls">
      <input type="text" placeholder="联系人QQ" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">联系人手机：</label>
   <div class="controls">
      <input type="text" placeholder="联系人手机" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">联系人EMAIL：</label>
   <div class="controls">
      <input type="text" placeholder="联系人EMAIL" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">营业执照号：</label>
   <div class="controls">
      <input type="text" placeholder="营业执照号" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">税务登记证号：</label>
   <div class="controls">
      <input type="text" placeholder="税务登记证号" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">组织机构代码证：</label>
   <div class="controls">
      <input type="text" placeholder="组织机构代码证" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">法定代表人：</label>
   <div class="controls">
      <input type="text" placeholder="法定代表人" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">法定代表人身份证号：</label>
   <div class="controls">
      <input type="text" placeholder="法定代表人身份证号" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">开户行名称：</label>
   <div class="controls">
      <input type="text" placeholder="开户行名称" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">开户行支行：</label>
   <div class="controls">
      <input type="text" placeholder="开户行支行" class="input-xfat input-xlarge">
   </div>
</div>

<div class="control-group">
   <label class="control-label">银行账号：</label>
   <div class="controls">
      <input type="text" placeholder="银行账号" class="input-xfat input-xlarge">
   </div>
</div>
```

修改sellerController.js ，在保存成功后跳转到登陆页

```javascript
//新增 
$scope.add=function(){
   sellerService.add( $scope.entity  ).success(
      function(response){
         if(response.success){
            //如果注册成功，跳转到登录页
              location.href="shoplogin.html";
         }else{
            alert(response.message);
         }
      }     
   );          
}
```

绑定“申请入驻”按钮

```html
<a class="sui-btn btn-block btn-xlarge btn-danger" ng-click="add()" target="_blank">申请入驻</a>
```

## 3.4后端代码

修改后端代码pinyougou-sellergoods-service的SellerServiceImpl类的add方法，设置默认状态为0

```java
/**
 * 增加
 * @param seller
 * @return
 */
@RequestMapping("/add")
public Result add(@RequestBody TbSeller seller){
   try {
      sellerService.add(seller);
      return new Result(true, "增加成功");
   } catch (Exception e) {
      e.printStackTrace();
      return new Result(false, "增加失败");
   }
}
```

测试

![1547052140177](assets/1547052140177.png)

![1547052153850](assets/1547052153850.png)

![1547052170063](assets/1547052170063.png)

