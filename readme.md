# 商品字段 JSON 结构定义

- **skuId**: 商品的ID
- **name**: 商品卡片中显示的title
- **price**: 商品价格
- **url**: 商品主图的链接
- **detailImagePath**: 商品详情图的链接，列表类型
- **mainImagePath**: 商品主图的链接，列表类型
- **comment**: 用户评论，列表类型
- **ocr**: 暂时可以忽略该字段

# 接口详细说明

## SearchInterface

- **描述**: 用于执行搜索操作的接口。
- **方法**: `get_response(prompt)`：向服务器发送搜索请求，并返回响应。
- **输入**: 一个关键词
- **输出**: 关于该关键词的搜索结果，有十个商品。

## RecomInterface

- **描述**: 用于执行推荐操作的接口。
- **方法**: `get_response(prompt=None)`：向服务器发送推荐请求，并返回响应。
- **输入**: 暂定为空，（后续会更新为用户的购买历史，当前的商品ID）
- **输出**: 返回十个商品。

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

在 `run_chat_test()` 函数中模拟了前端的操作，以及如何调用 API，可以后端实现的流程。每次调用 API 时后端的准备代码都是一样的：
```python
prompt = app.load_data_from_disk()
```

然后第 79 行，第 88-97 行，和第 106 行均来自前端的响应。对于前端响应，有统一的后端代码：
```python
return_data = app.get_response(prompt)
app.post_process(prompt, return_data)
```