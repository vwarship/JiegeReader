## 杰哥阅读器

### Backlog

* 增加内容提供者保存数据。
  
* 文章显示时间。（统一成10分钟前，1小时前，昨天，前天）

* channel 中的 <lastBuildDate> 代表内容最后的修改时间。（可选标签）

* 优化 WebView

### 0.3.0 - 2015-3-5

* 编写了 ViewHolder 类，提高 ListView 展示的效率。

* 解读时间格式：
  * 2015-03-04 01:22:20
  * Wed, 04 Mar 2015 00:00:00 +0800

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

------------
public class NewsArrayAdapter extends ArrayAdapter<Item> {
    private int resource;

    public NewsArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(resource, parent, false);
        }

        Item news = getItem(position);

        TextView newsTitle = (TextView)view.findViewById(R.id.tvNewsTitle);
        newsTitle.setText(news.getTitle());

        TextView newsDate = (TextView)view.findViewById(R.id.tvNewsDate);
        newsDate.setText(news.getPubDate());

        return view;
    }
}


public class NewsArrayAdapter extends ArrayAdapter<Item> {
    private int resource;
    private LayoutInflater inflater;

    public NewsArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        class ViewHolder {
            TextView title;
            TextView date;
        }
        final ViewHolder viewHolder;

        if (convertView == null) {
            view = inflater.inflate(resource, null);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView)view.findViewById(R.id.tvNewsTitle);
            viewHolder.date = (TextView)view.findViewById(R.id.tvNewsDate);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }

        Item news = getItem(position);

        viewHolder.title.setText(news.getTitle());
        viewHolder.date.setText(news.getPubDate());

        return view;
    }
}


        newsArrayAdapter = new QuickAdapter<Item>(this, R.layout.news_list_item) {
            @Override
            protected void convert(BaseAdapterHelper baseAdapterHelper, Item item) {
                baseAdapterHelper.setText(R.id.tvNewsTitle, item.getTitle())
                        .setText(R.id.tvNewsDate, item.getPubDate());
            }
        };
