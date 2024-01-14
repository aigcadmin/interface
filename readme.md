# 商品字段 JSON 结构定义

- **skuId**: 商品的ID
- **name**: 商品卡片中显示的标题
- **price**: 商品价格
- **url**: 商品主图的链接
- **detailImagePath**: 商品详情图的路径，列表类型
- **mainImagePath**: 商品主图的路径，列表类型
- **comment**: 用户评论，列表类型
- **ocr**: 暂时可以忽略该字段

<!-- - **id**: 商品的ID
- **store_name**: 商品卡片中显示的title
- **price**: 商品价格
- **image**: 商品主图的链接，列表类型
- **slider_image**: 商品详情图的链接，列表类型
- **comment**: 用户评论，列表类型
- **cate_id**: 商品的字类别 -->

### 关于图片的加载

商品的图片全部通过 AWS 云端 CDN 加速来实现。算法返回的商品图片列表是云端路径，其前缀为：

[https://aigcadminimagebucket.s3.us-east-2.amazonaws.com/](https://aigcadminimagebucket.s3.us-east-2.amazonaws.com/)


例如，如果商品返回的图片路径是 `img_dir_516/EN/jdItemImage_item_0_id_0.jpg`，那么相应的图片链接就是：

[https://aigcadminimagebucket.s3.us-east-2.amazonaws.com/img_dir_516/EN/jdItemImage_item_0_id_0.jpg](https://aigcadminimagebucket.s3.us-east-2.amazonaws.com/img_dir_516/EN/jdItemImage_item_0_id_0.jpg)


<br>

# 接口详细说明

## SearchInterface

- **描述**: 用于执行搜索操作的接口，用于搜索结果页面。
- **方法**: `get_response(prompt)`：向服务器发送搜索请求，并返回响应。
- **输入**: 一个关键词，字符串类型数据。
- **输出**: 关于该关键词的搜索结果，有20个商品。
- **后续更新**：后续会更新刷新的API，就是提供了20个商品后用户希望浏览更多的商品时调用的接口方式。

<br>

## RecomInterface

<!-- - **描述**: 用于执行推荐操作的接口。推荐页面有三种类型：在主页面，在商品详情页后，以及在购物车页面后。
- **方法**: `get_response(user_id, product_id)`：向服务器发送推荐请求，并返回响应。
- **输入**: 用户的ID和当前的商品ID。
  当用户ID为0时代表未登陆状态，用户为匿名。
  当商品ID为0时代表主页面的推荐，商品ID为-1时代表购物车页面的推荐，商品ID为正时代表正常的商品ID。
- **输出**: 返回十个商品。 -->

### 描述
此接口用于执行推荐操作。推荐功能适用于四种页面类型：
1. 主页面(Home), product_id=0, user_id允许为0，代表未登陆状态
2. 商品详情页面后, product_id=product_id，user_id允许为0，代表未登陆状态
3. 购物车页面(Cart), product_id=-1，user_id必须为真实用户ID，不可以匿名状态
4. 用户个人页面(Me), product_id=0，user_id必须为真实用户ID，不可以匿名状态

### 方法
- `get_response(user_id, product_id)`
  - 功能：向服务器发送推荐请求，并返回响应。

### 输入参数
- `user_id`：用户的ID
  - `0` 表示未登录状态，用户为匿名。
  - 正值表示正常的用户ID。
- `product_id`：当前的商品ID
  - `0` 表示主页面(Home)或者个人页面(Me)的推荐。
  - `-1` 表示购物车页面(Cart)的推荐。
  - 正值表示正常的商品详情页面后的推荐，值代表该商品ID。

### 返回数据
- 返回10个推荐商品的列表。

<br>
<!-- <br> -->

## ChatInterface

- **描述**: 用于执行对话操作的接口。
- **初始化**: 使用类的构造函数创建一个实例，设置服务器地址、案例 ID 和文件路径。
- **方法**:
  - `load_data_from_disk()`: 从磁盘加载对话数据，如果该对话从未初始化过，那么加载模板 JSON 文件，否则加载历史对话 JSON 文件。在实际实现中，可以根据每一个用户的每一个对话创建一个该文件，然后维护相应的文件。或者也可以用数据库的形式保存。
  - `get_response(prompt)`: 向服务器发送对话请求，并返回响应。
- **输入**: 从硬盘中加载的 JSON 文件。
- **输出**: 一个字典，包含 "flag" 和 "info" 两种 key。

- **post_process(prompt, return_data)**: 处理从服务器返回的数据。返回的数据为一个字典，包含 "flag" 和 "info" 两个字段。根据 "flag" 分为以下四种类型，每种类型有不同的后处理方法：
  1. "text": 文本流信息，需要在前端显示相应的文本流
  2. "select": 选项 tag，包含题目和选项，需要在前端显示
  3. "commodity": 商品列表信息，需要在前端展示商品卡片
  4. "log": 将 "info" 的信息作为历史记录，保存到硬盘或内存中

在 `run_chat_test()` 函数中模拟了前端的操作，和如何调用 API，以及后端实现的流程。每次调用 API 时后端的准备代码都是一样的：
```python
prompt = app.load_data_from_disk()
```

然后第 79 行，第 88-97 行，和第 106 行均来自前端的响应。对于前端响应，有统一的后端代码：
```python
return_data = app.get_response(prompt)
app.post_process(prompt, return_data)
```