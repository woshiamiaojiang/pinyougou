# 课程目标

目标1：实现品优购搜索结果高亮显示功能

目标2：说出品优购搜索的业务规则和实现思路

目标3：完成查询分类列表的功能

目标4：完成缓存品牌和规格数据的功能

目标5：完成显示品牌和规格数据的功能

目标6：完成过滤条件构建的功能

目标7：完成过滤查询的功能

# 1.品优购-高亮显示

## 1.1需求分析

将用户输入的关键字在标题中以红色的字体显示出来，就是搜索中常用的高亮显示.

![img](assets/clip_image002-1548659569397.jpg)

## 1.2后端代码

修改服务层代码ItemSearchServiceImpl.java 

创建私有方法，用于返回查询列表的结果（高亮）

修改ItemSearchServiceImpl 的search方法，调用刚才编写的私有方法

```java
package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightPage;

import java.util.HashMap;
import java.util.Map;
@Service(timeout=5000)
public class ItemSearchServiceImpl implements ItemSearchService {

   @Autowired
   private SolrTemplate solrTemplate;

   @Override
   public Map<String, Object> search(Map searchMap) {
      Map<String,Object> map=new HashMap<>();
      //1.查询列表      
      map.putAll(searchList(searchMap));
      return map;
   }


   /**
    * 根据关键字搜索列表
    * @param keywords
    * @return
    */
   private Map searchList(Map searchMap){
      Map map=new HashMap();
      HighlightQuery query=new SimpleHighlightQuery();
      HighlightOptions highlightOptions=new HighlightOptions().addField("item_title");//设置高亮的域
      highlightOptions.setSimplePrefix("<em style='color:red'>");//高亮前缀
      highlightOptions.setSimplePostfix("</em>");//高亮后缀
      query.setHighlightOptions(highlightOptions);//设置高亮选项
      //按照关键字查询
      Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
      query.addCriteria(criteria);
      HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
      for(HighlightEntry<TbItem> h: page.getHighlighted()){//循环高亮入口集合
         TbItem item = h.getEntity();//获取原实体类
         if(h.getHighlights().size()>0 && h.getHighlights().get(0).getSnipplets().size()>0){
            item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));//设置高亮的结果
         }
      }
      map.put("rows",page.getContent());
      return map;
   }


}
```

## 1.3前端代码

我们测试后发现高亮显示的html代码原样输出，这是angularJS为了防止html攻击采取的安全机制。我们如何在页面上显示html的结果呢？我们会用到$sce服务的trustAsHtml方法来实现转换。

因为这个功能具有一定通用性，我们可以通过angularJS的过滤器来简化开发，这样只写一次，调用的时候就非常方便了，看代码：

（1）修改base.js 

```js
// 定义模块:
var app = angular.module("pinyougou",[]);
/*$sce服务写成过滤器*/
app.filter('trustHtml',['$sce',function($sce){
    return function(data){
        return $sce.trustAsHtml(data);
    }
}]);
```

（2）使用过滤器

ng-bind-html指令用于显示html内容

竖线 |用于调用过滤器

```html
<div class="attr" ng-bind-html="item.title | trustHtml"></div>
```

*|**就是竖线，看起来有点斜是因为字体原因。*

测试

![1548673231253](assets/1548673231253.png)

# 2.搜索业务规则分析

## 2.1需求分析

我们今天要完成的目标是在关键字搜索的基础上添加面板搜索功能。

面板上有商品分类、品牌、各种规格和价格区间等条件

![img](assets/clip_image002-1548669619000.jpg)

业务规则：

（1）当用户输入关键字搜索后，除了显示列表结果外，还应该显示通过这个关键字搜索到的记录都有哪些商品分类。

（2）根据第一个商品分类查询对应的模板，根据模板查询出品牌列表

（3）根据第一个商品分类查询对应的模板，根据模板查询出规格列表

（4）当用户点击搜索面板的商品分类时，显示按照这个关键字查询结果的基础上，筛选此分类的结果。

（5）当用户点击搜索面板的品牌时，显示在以上结果的基础上，筛选此品牌的结果

（6）当用户点击搜索面板的规格时，显示在以上结果的基础上，筛选此规格的结果

（7）当用户点击价格区间时，显示在以上结果的基础上，按价格进行筛选的结果

（8）当用户点击搜索面板的相应条件时，隐藏已点击的条件。

## 2.2实现思路

（1）搜索面板的商品分类需要使用Spring Data Solr的分组查询来实现

（2）为了能够提高查询速度，我们需要把查询面板的品牌、规格数据提前放入redis

（3）查询条件的构建、面板的隐藏需要使用angularJS来实现

（4）后端的分类、品牌、规格、价格区间查询需要使用过滤查询来实现

# 3.查询分类列表

## 3.1需求分析

根据搜索关键字查询商品分类名称列表

![img](assets/clip_image004-1548669619001.jpg)

## 3.2后端代码

修改SearchItemServiceImpl.java创建方法   

```java
/**
 * 查询分类列表
 * @param searchMap
 * @return
 */
private  List searchCategoryList(Map searchMap){
   List<String> list=new ArrayList();
   Query query=new SimpleQuery();
   //按照关键字查询
   Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
   query.addCriteria(criteria);
   //设置分组选项
   GroupOptions groupOptions=new GroupOptions().addGroupByField("item_category");
   query.setGroupOptions(groupOptions);
   //得到分组页
   GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
   //根据列得到分组结果集
   GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
   //得到分组结果入口页
   Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
   //得到分组入口集合
   List<GroupEntry<TbItem>> content = groupEntries.getContent();
   for(GroupEntry<TbItem> entry:content){
      list.add(entry.getGroupValue());//将分组结果的名称封装到返回值中
   }
   return list;
}
```

search方法调用

```java
@Override
public Map<String, Object> search(Map searchMap) {
   Map<String,Object> map=new HashMap<>();
   //1.查询列表
   map.putAll(searchList(searchMap));

   //2.根据关键字查询商品分类
   List categoryList = searchCategoryList(searchMap);
   map.put("categoryList",categoryList);

   return map;
}
```

## 3.3前端代码

修改search.html

```html
<div class="type-wrap" ng-if="resultMap.categoryList!=null">
    <div class="fl key">商品分类</div>
    <div class="fl value">
        <span ng-repeat="category in resultMap.categoryList">
            <a href="#">{{category}}</a>
        </span>
    </div>
    <div class="fl ext"></div>
</div>
```

测试

![1548688054998](assets/1548688054998.png)

# 4.缓存品牌和规格数据

## 4.1需求分析

将商品分类数据、品牌数据、和规格数据都放入Redis存储。

（1）当用户进入运营商后台的商品分类页面时，将商品分类数据放入缓存（Hash）。以分类名称作为key ,以模板ID作为值

（2）当用户进入运营商后台的模板管理页面时，分别将品牌数据和规格数据放入缓存（Hash）。以模板ID作为key,以品牌列表和规格列表作为值。

## 4.2缓存商品分类数据

将商品分类表存入缓存  pinyougou-sellergoods-service工程需要引入pinyougou-common工程依赖。

修改pinyougou-sellergoods-service的ItemCatServiceImpl.java，添加redisTemplate

```java
/**
 * 根据上级ID查询列表
 */
@Override
public List<TbItemCat> findByParentId(Long parentId) {
   TbItemCatExample example1=new TbItemCatExample();
   Criteria criteria1 = example1.createCriteria();
   criteria1.andParentIdEqualTo(parentId);
   //每次执行查询的时候，一次性读取缓存进行存储 (因为每次增删改都要执行此方法)
   List<TbItemCat> list = findAll();
   for(TbItemCat itemCat:list){
      redisTemplate.boundHashOps("itemCat").put(itemCat.getName(), itemCat.getTypeId());
   }
   System.out.println("更新缓存:商品分类表");

   return  itemCatMapper.selectByExample(example1);
}

@Autowired
private RedisTemplate redisTemplate;
```

## 4.3缓存品牌和规格列表数据

（1）修改pinyougou-sellergoods-service的TypeTemplateServiceImpl.java 

```java
@Autowired
private RedisTemplate redisTemplate;

/**
 * 将数据存入缓存
 */
private void saveToRedis(){
   //获取模板数据
   List<TbTypeTemplate> typeTemplateList = findAll();
   //循环模板
   for(TbTypeTemplate typeTemplate :typeTemplateList){
      //存储品牌列表      
      List<Map> brandList = JSON.parseArray(typeTemplate.getBrandIds(), Map.class);
      redisTemplate.boundHashOps("brandList").put(typeTemplate.getId(), brandList);
      //存储规格列表
      List<Map> specList = findSpecList(typeTemplate.getId());//根据模板ID查询规格列表
      redisTemplate.boundHashOps("specList").put(typeTemplate.getId(), specList);
   }
}
```

（2）在查询分页方法(findPage) 时调用此方法  

```java
Page<TbTypeTemplate> page= (Page<TbTypeTemplate>)typeTemplateMapper.selectByExample(example);
saveToRedis();//存入数据到缓存
return new PageResult(page.getTotal(), page.getResult());
```

这样在增删改后会自动调用该方法.

![1548733376442](assets/1548733376442.png)



![1548733392818](assets/1548733392818.png)

## 4.4加载缓存数据

启动redis  ,运行运营商管理后台，打开商品分类和模板管理页，即可将数据放入缓存中。

# 5.显示品牌和规格数据

## 5.1需求分析

在搜索面板区域显示第一个分类的品牌和规格列表

![img](assets/clip_image002-1548731838850.jpg)

## 5.2后端代码

修改ItemSearchServiceImpl.java ，增加方法

```java
@Autowired
private RedisTemplate redisTemplate;

/**
 * 查询品牌和规格列表
 * @param category 分类名称
 * @return
 */
private Map searchBrandAndSpecList(String category){
   Map map=new HashMap();
   Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);//获取模板ID
   if(typeId!=null){
      //根据模板ID查询品牌列表
      List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
      map.put("brandList", brandList);//返回值添加品牌列表
      //根据模板ID查询规格列表
      List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
      map.put("specList", specList);
   }
   return map;
}
```

Search方法调用此方法

```java
@Override
public Map<String, Object> search(Map searchMap) {
    Map<String,Object> map=new HashMap<>();
    //1.查询列表
    map.putAll(searchList(searchMap));

    //2.根据关键字查询商品分类
    List<String> categoryList = searchCategoryList(searchMap);
    map.put("categoryList",categoryList);
    //3.查询品牌和规格列表
    if(categoryList.size()>0){
        map.putAll(searchBrandAndSpecList(categoryList.get(0)));
    }

    return map;
}

```

## 5.3前端代码

### 5.3.1获取品牌列表

修改页面search.html，实现品牌列表

```html
<div class="type-wrap logo" ng-if="resultMap.brandList!=null">
   <div class="fl key brand">品牌</div>
   <div class="value logos">
      <ul class="logo-list">
         <li ng-repeat="brand in resultMap.brandList">
            {{brand.text}}
         </li>
      </ul>
   </div>
```

### 5.3.2获取规格列表

修改页面search.html，实现规格列表

```html
<div class="type-wrap logo" ng-if="resultMap.brandList!=null">
   <div class="fl key brand">品牌</div>
   <div class="value logos">
      <ul class="logo-list">
         <li ng-repeat="brand in resultMap.brandList">
            {{brand.text}}
         </li>
      </ul>
   </div>

   <div class="ext">
      <a href="javascript:void(0);" class="sui-btn">多选</a>
      <a href="javascript:void(0);">更多</a>
   </div>
</div>
<div class="type-wrap" ng-repeat="spec in resultMap.specList">
   <div class="fl key">{{spec.text}}</div>
   <div class="fl value">
      <ul class="type-list">
         <li ng-repeat="pojo in spec.options">
            <a>{{pojo.optionName}}</a>
         </li>
      </ul>
   </div>
   <div class="fl ext"></div>
</div>
```

测试

![1548734300645](assets/1548734300645.png)

# 6.过滤条件构建

## 6.1需求分析

点击搜索面板上的分类、品牌和规格，实现查询条件的构建。查询条件以面包屑的形式显示。

当面包屑显示分类、品牌和规格时，要同时隐藏搜索面板对应的区域。

用户可以点击面包屑上的X 撤销查询条件。撤销后显示搜索面包相应的区域。

## 6.2添加搜索项

### 6.2.1添加搜索项方法

修改pinyougou-search-web的searchController.js 

```js
$scope.searchMap={'keywords':'','category':'','brand':'','spec':{}};//搜索对象
//添加搜索项
$scope.addSearchItem=function(key,value){
    if(key=='category' || key=='brand'){//如果点击的是分类或者是品牌
        $scope.searchMap[key]=value;
    }else{
        $scope.searchMap.spec[key]=value;
    }
}
```

### 6.2.2点击搜索项

修改pinyougou-search-web 的search.html ，为搜索面板添加点击事件

点击商品分类标签

```html
<a href="#" ng-click="addSearchItem('category',category)">{{category}}</a>
```

点击品牌标签

```html
<a href="#" ng-click="addSearchItem('brand',brand.text)">{{brand.text}}</a>
```

点击规格标签

```html
<a href="#"  ng-click="addSearchItem(spec.text,pojo.optionName)">
   {{pojo.optionName}}</a>
```

### 6.2.3显示面包屑

修改pinyougou-search-web 的search.html，用面包屑形式显示搜索条件

```html
<ul class="fl sui-breadcrumb">搜索条件：</ul>
<ul class="tags-choose">
   <li class="tag" ng-if="searchMap.category!=''">商品分类：{{searchMap.category}}<i class="sui-icon icon-tb-close"></i></li>
   <li class="tag" ng-if="searchMap.brand!=''">品牌：{{searchMap.brand}}<i class="sui-icon icon-tb-close"></i></li>
   <li class="tag" ng-repeat="(key,value) in searchMap.spec">{{key}}:{{value}}<i class="sui-icon icon-tb-close"></i></li>
</ul>
```

测试

![1548736662744](assets/1548736662744.png)

## 6.3撤销搜索项

### 6.3.1撤销搜索项的方法

修改pinyougou-search-web工程searchController.js

```js
//移除复合搜索条件
$scope.removeSearchItem=function(key){
    if(key=="category" ||  key=="brand"){//如果是分类或品牌
        $scope.searchMap[key]="";
    }else{//否则是规格
        delete $scope.searchMap.spec[key];//移除此属性
    }
}
```

### 6.3.2页面调用方法

pinyougou-search-web工程的search.html

```html
<ul class="tags-choose">
   <li class="tag" ng-if="searchMap.category!=''" ng-click="removeSearchItem('category')">商品分类：{{searchMap.category}}<i class="sui-icon icon-tb-close"></i></li>
   <li class="tag" ng-if="searchMap.brand!=''" ng-click="removeSearchItem('brand')">品牌：{{searchMap.brand}}<i class="sui-icon icon-tb-close"></i></li>
   <li class="tag" ng-repeat="(key,value) in searchMap.spec" ng-click="removeSearchItem(key)">{{key}}:{{value}}<i class="sui-icon icon-tb-close"></i></li>
</ul>
```

测试

![1548741715040](assets/1548741715040.png)

## 6.4隐藏查询面板

### 6.4.1隐藏分类面板

修改search.html 

```html
<div class="type-wrap" ng-if="resultMap.categoryList!=null && searchMap.category==''">
   <div class="fl key">商品分类</div>
   <div class="fl value">
   <span ng-repeat="category in resultMap.categoryList">
      <a href="#" ng-click="addSearchItem('category',category)">{{category}}</a>
   </span>
   </div>
   <div class="fl ext"></div>
</div>
```

### 6.4.2隐藏品牌面板

修改search.html 

```html
<div class="type-wrap logo" ng-if="resultMap.brandList!=null && searchMap.brand==''">
   <div class="fl key brand">品牌</div>
```

### 6.4.3隐藏规格面板

修改search.html 

```html
<div class="type-wrap" ng-repeat="spec in resultMap.specList" ng-if="searchMap.spec[spec.text]==null">
   <div class="fl key">{{spec.text}}</div>
```

## 6.5提交查询

修改searchController.js 在添加和删除筛选条件时自动调用搜索方法

```js
//添加复合搜索条件
$scope.addSearchItem=function(key,value){
    if(key=="category" ||  key=="brand"){//如果是分类或品牌
        $scope.searchMap[key]=value;
    }else{//否则是规格
        $scope.searchMap.spec[key]=value;
    }
    $scope.search();//执行搜索 
}
//移除复合搜索条件
$scope.removeSearchItem=function(key){
    if(key=="category" ||  key=="brand"){//如果是分类或品牌
        $scope.searchMap[key]="";
    }else{//否则是规格
        delete $scope.searchMap.spec[key];//移除此属性
    }
    $scope.search();//执行搜索 
}
```

测试

![1548742187616](assets/1548742187616.png)

# 7.过滤查询

## 7.1需求分析

根据上一步构建的查询条件，实现分类、品牌和规格的过滤查询

![img](assets/clip_image002-1548741924577.jpg)

## 7.2代码实现

### 7.2.1分类过滤

修改pinyougou-search-service工程的SearchItemServiceImpl.java 

```java
//1.2按分类筛选
if(!"".equals(searchMap.get("category"))){
   Criteria filterCriteria=new Criteria("item_category").is(searchMap.get("category"));
   FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
   query.addFilterQuery(filterQuery);
}
```

测试

![1548743203745](assets/1548743203745.png)

### 7.2.2品牌过滤

修改pinyougou-search-service工程的SearchItemServiceImpl.java 

```java
//1.3按品牌筛选
if(!"".equals(searchMap.get("brand"))){
   Criteria filterCriteria=new Criteria("item_brand").is(searchMap.get("brand"));
   FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
   query.addFilterQuery(filterQuery);
}
```

测试

![1548743304292](assets/1548743304292.png)



### 7.2.3规格过滤

实现思路：规格有多项，需要循环过滤。循环规格查询条件，根据key得到域名城，根据value设置过滤条件。

修改pinyougou-search-service工程的SearchItemServiceImpl.java 

```java
//1.4过滤规格
if(searchMap.get("spec")!=null){
   Map<String,String> specMap= (Map) searchMap.get("spec");
   for(String key:specMap.keySet() ){
      Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key) );
      FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
      query.addFilterQuery(filterQuery);
   }
}
```

![1548743432741](assets/1548743432741.png)

### 7.2.4根据分类查询品牌规格列表

```java
//3.查询品牌和规格列表
String categoryName=(String)searchMap.get("category");
if(!"".equals(categoryName)){//如果有分类名称
   map.putAll(searchBrandAndSpecList(categoryName));
}else{//如果没有分类名称，按照第一个查询
   if(categoryList.size()>0){
      map.putAll(searchBrandAndSpecList(categoryList.get(0)));
   }
}
```

测试

![1548743497861](assets/1548743497861.png)

