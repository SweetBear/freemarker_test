<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>博客列表</title>
    <!-- 通过CDN引入Vue.js -->
    <script src="https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.js"></script>
    <!-- 通过CDN引入axios用于异步请求 -->
    <script src="https://cdn.jsdelivr.net/npm/axios@0.21.1/dist/axios.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: "Microsoft YaHei", Arial, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 800px; margin: 0 auto; }
        h1 { text-align: center; color: #333; margin-bottom: 30px; padding-bottom: 15px; border-bottom: 3px solid #007bff; }
        .blog-item { background: #fff; border-radius: 6px; padding: 20px; margin-bottom: 15px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); transition: box-shadow 0.3s; }
        .blog-item:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
        .blog-title { font-size: 18px; font-weight: bold; color: #333; text-decoration: none; display: block; margin-bottom: 8px; }
        .blog-title:hover { color: #007bff; }
        .blog-meta { color: #888; font-size: 13px; margin-bottom: 5px; }
        .blog-meta span { margin-right: 15px; }
        .read-count-area { margin-top: 8px; font-size: 14px; }
        .read-count { color: #007bff; cursor: pointer; font-weight: bold; }
        .read-count:hover { text-decoration: underline; }
        .loading-text { color: #999; font-style: italic; }
        .error-text { color: #dc3545; }
        .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }
    </style>
</head>
<body>
    <div id="app" class="container">
        <h1>博客文章列表</h1>

        <#list blogs as blog>
        <div class="blog-item">
            <a class="blog-title" href="/getStaticPage/blog${blog.id}.html">${blog.title?html}</a>
            <div class="blog-meta">
                <span>作者: ${blog.author?html}</span>
                <span>发布时间: ${blog.publishTime?html}</span>
                <span>分类: ${blog.category?html}</span>
            </div>
            <div class="read-count-area">
                阅读数:
                <span v-if="counts[${blog.id}] === undefined" class="loading-text">加载中...</span>
                <span v-else class="read-count" @click="refresh(${blog.id})" title="点击刷新阅读数">{{ counts[${blog.id}] }}</span>
            </div>
        </div>
        </#list>

        <div class="footer">
            <p>页面静态化示例 &mdash; 阅读数为实时异步获取数据</p>
        </div>
    </div>

    <script>
        // 初始化Vue实例，用于异步获取和渲染阅读数
        new Vue({
            el: '#app',
            data: {
                counts: {}  // 存储各blog的阅读数，key为blogId，value为阅读数
            },
            mounted: function() {
                // 页面加载完成后自动请求所有blog的阅读数
                this.fetchAll();
            },
            methods: {
                // 获取所有blog的阅读数
                fetchAll: function() {
                    var blogIds = [<#list blogs as blog>${blog.id}<#if blog_has_next>,</#if></#list>];
                    var self = this;
                    blogIds.forEach(function(id) {
                        self.fetchOne(id);
                    });
                },
                // 获取单个blog的阅读数
                fetchOne: function(id) {
                    var self = this;
                    axios.get('/getBlogReadCount/' + id)
                        .then(function(response) {
                            if (response.data && response.data.code === 200) {
                                self.$set(self.counts, id, response.data.data);
                            } else {
                                self.$set(self.counts, id, 0);
                            }
                        })
                        .catch(function() {
                            // 请求失败时显示默认阅读数
                            self.$set(self.counts, id, 0);
                        });
                },
                // 点击阅读数手动刷新
                refresh: function(id) {
                    this.fetchOne(id);
                }
            }
        });
    </script>
</body>
</html>
