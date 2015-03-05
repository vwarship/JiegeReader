﻿## 杰哥阅读器

### Backlog

* 增加SQLite保存数据。

* 用户可以手动更新新闻。

* 打开应用，在后台自动更新。

* 增加首屏 Activity

* 检查RSS是否更新。

* 查看后将自动删除文章。

* 可以分享社交应用上。（朋友圈、微博等）

* 文章超过3天后将自动删除。

* 可以通过左抽屉查看单个RSS源。
  * 每个RSS源显示文章数。
  * 最下面显示文章最后更新时间。

* channel 中的 <lastBuildDate> 代表内容最后的修改时间。（可选标签）

* 优化 WebView

### 0.4.0 - 2015-3-6

* 时间格式化成可读的。如：刚刚、1分钟前、1小时前、昨天、03-05 21:09

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
