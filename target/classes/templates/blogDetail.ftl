<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${blog.title?html} - 博客详情</title>
    <!-- 通过CDN引入Vue.js -->
    <script src="https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.js"></script>
    <!-- 通过CDN引入axios用于异步请求 -->
    <script src="https://cdn.jsdelivr.net/npm/axios@0.21.1/dist/axios.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: "Microsoft YaHei", Arial, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 800px; margin: 0 auto; }
        .blog-header { background: #fff; border-radius: 6px; padding: 25px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .blog-title { font-size: 24px; color: #333; margin-bottom: 12px; line-height: 1.4; }
        .blog-meta { color: #888; font-size: 14px; }
        .blog-meta span { margin-right: 20px; }
        .read-count-area { background: #fff; border-radius: 6px; padding: 15px 25px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); font-size: 16px; }
        .read-count { color: #007bff; cursor: pointer; font-weight: bold; font-size: 20px; }
        .read-count:hover { text-decoration: underline; }
        .loading-text { color: #999; font-style: italic; }
        .blog-content { background: #fff; border-radius: 6px; padding: 25px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); line-height: 1.8; font-size: 15px; color: #444; }
        .blog-content p { margin-bottom: 15px; }
        .back-area { margin-top: 25px; text-align: center; }
        .back-link { display: inline-block; padding: 10px 25px; background: #007bff; color: #fff; text-decoration: none; border-radius: 4px; transition: background 0.3s; }
        .back-link:hover { background: #0056b3; }
        .footer { text-align: center; margin-top: 25px; color: #999; font-size: 12px; }
    </style>
</head>
<body>
    <div id="app" class="container">
        <div class="blog-header">
            <h1 class="blog-title">${blog.title?html}</h1>
            <div class="blog-meta">
                <span>作者: ${blog.author?html}</span>
                <span>发布时间: ${blog.publishTime?html}</span>
                <span>分类: ${blog.category?html}</span>
            </div>
        </div>

        <!-- 阅读数区域，通过Vue异步获取并渲染，支持点击刷新 -->
        <div class="read-count-area">
            阅读数:
            <span v-if="loading" class="loading-text">加载中...</span>
            <span v-else class="read-count" @click="refreshReadCount" title="点击刷新阅读数">{{ readCount }}</span>
        </div>

        <div class="blog-content">
            <p>${blog.content?html}</p>
        </div>

        <div class="back-area">
            <a class="back-link" href="/getStaticPage/blogList.html">返回博客列表</a>
        </div>

        <div class="footer">
            <p>页面静态化示例 &mdash; 阅读数为实时异步获取数据</p>
        </div>
    </div>

    <script>
        // 初始化Vue实例，异步获取并渲染阅读数
        new Vue({
            el: '#app',
            data: {
                blogId: ${blog.id},   // 当前blog的ID，用于请求对应的阅读数
                readCount: 0,          // 阅读数初始值
                loading: true          // 加载状态标识
            },
            mounted: function() {
                // 页面加载完成后自动请求阅读数
                this.fetchReadCount();
            },
            methods: {
                // 异步请求后端接口获取实时阅读数
                fetchReadCount: function() {
                    var self = this;
                    axios.get('/getBlogReadCount/' + self.blogId)
                        .then(function(response) {
                            if (response.data && response.data.code === 200) {
                                self.readCount = response.data.data;
                            }
                        })
                        .catch(function() {
                            // 请求异常时显示默认阅读数
                            self.readCount = 0;
                        })
                        .finally(function() {
                            self.loading = false;
                        });
                },
                // 点击阅读数手动刷新
                refreshReadCount: function() {
                    this.loading = true;
                    this.fetchReadCount();
                }
            }
        });
    </script>
</body>
</html>
