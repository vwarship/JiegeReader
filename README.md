﻿## 杰哥阅读器

### Backlog

* 增加首屏 Activity

* 检查RSS是否更新。

* 可以分享社交应用上。（朋友圈、微博等）

* 可以通过左抽屉查看单个RSS源。
  * 每个RSS源显示文章数。
  * 最下面显示文章最后更新时间。

* 支持中英文。
  * 文章的时间需要用资源来解决复数。

* channel 中的 <lastBuildDate> 代表内容最后的修改时间。（可选标签）

* 增加 Atom 解析

* 优化 WebView

### 0.15.0 - 2015-3-22

* 可以手动删除新闻。

* 查看过的新闻将使用灰色显示标题。

* 增加了RSS，修改了来源的名字。

* 第一次安装后，抽屉是关闭的。

* 订阅 RSS 后，自动更新下载列表。

* 数据库优化，创建了索引。

* 应用启动后自动下载更新。

* 文章超过3天后将自动删除。

### 0.14.0 - 2015-3-16

* RSS数据库增加了更新日期字段。

### 0.13.0 - 2015-3-15

* 更新回顶部的图标。

* 导航栏打开便更新。

### 0.12.0 - 2015-3-14

* 解决网络未连接出现崩溃。

* 立即下载RSS内容。

### 0.11.0 - 2015-3-13

* 增加可以快速回到新闻顶部的功能。

* 为回到顶部按钮增加了振动。

### 0.10.0 - 2015-3-12

* 修复了同时使用异步任务下载网页和读取数据库不能同时进行。

* 在开始时下滑 ListView 讲重新下面网页，更新新闻。

* 显示新闻的 ListView 滚动到最下面时，自动加载下20条。

### 0.9.0 - 2015-3-11

* 增加【再按一次，退出程序】

### 0.8.0 - 2015-3-10

* 增加通过已安装应用分享

### 0.7.0 - 2015-3-9

* 修改了样式。

### 0.6.0 - 2015-3-8

* 用户在订阅中心可以订阅和取消订阅 RSS 频道。

* 查询新闻内容使用异步。

* 增加了显示新闻来源。

* 第一次显示前20条新闻。

* 增加了导航栏。

* 只能竖屏阅读。

### 0.5.0 - 2015-3-7

* 新闻按发布的时间降序显示。

* 增加了RSS订阅的数据库存储。

* 增加了从RSS订阅表中读取RSS地址，在后台定时下载。

* RSS 订阅中心显示来自数据库。

### 0.4.0 - 2015-3-6

* 增加SQLite保存数据和查询的功能。

* 增加了新闻去重。

* 时间格式化成可读的。
  * 1分钟内显示：刚刚
  * 1分钟到1小时显示：X分钟前
  * 1小时到24小时显示：X小时前
  * 1天到2天显示：昨天
  * 超过2天显示：03-05 21:09

### 0.3.0 - 2015-3-5

* 编写了 ViewHolder 类，提高 ListView 展示的效率。

* 解读时间格式：
  * 2015-03-04 01:22:20
  * Wed, 04 Mar 2015 00:00:00 +0800

* 优化 WebView。使用单独进程的方式解决了影响现有进程的问题。

### 0.2.0 - 2015-3-4

* 增加订阅中心界面

### 0.1.0 - 2015-3-3

* 可以下载文章。

* 增加了对 HTML 编码的探测，解决乱码的问题。

* 解读 RSS 2.0

* 将文章列表显示到 ListView 上。

* 单击文章可以在应用内查看原文。

* 制作了图标。

* 增加了中英文的支持。
