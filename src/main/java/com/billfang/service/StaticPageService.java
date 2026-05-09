package com.billfang.service;

import com.billfang.entity.Blog;
import com.billfang.util.FreemarkerUtil;
import com.billfang.util.GridFsUtil;
import com.billfang.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 静态页面生成服务，负责项目启动时自动生成静态页面并上传至GridFS，
 * 以及数据变更后的同步操作（重新生成/删除GridFS中的静态页）
 */
@Service
public class StaticPageService {

    private static final Logger logger = LoggerFactory.getLogger(StaticPageService.class);

    private final JsonUtil jsonUtil;
    private final FreemarkerUtil freemarkerUtil;
    private final GridFsUtil gridFsUtil;

    public StaticPageService(JsonUtil jsonUtil, FreemarkerUtil freemarkerUtil, GridFsUtil gridFsUtil) {
        this.jsonUtil = jsonUtil;
        this.freemarkerUtil = freemarkerUtil;
        this.gridFsUtil = gridFsUtil;
        // 注册JSON文件外部修改监听
        jsonUtil.registerChangeListener(this::onFileChanged);
    }

    /**
     * 生成所有静态页面：列表页 + 每篇blog的详情页
     */
    public void generateAllPages() throws Exception {
        List<Blog> blogs = jsonUtil.getAllBlogs();
        logger.info("开始生成静态页面，共{}篇blog", blogs.size());

        // 生成每篇blog的详情页
        for (Blog blog : blogs) {
            generateDetailPage(blog);
        }

        // 生成列表页
        generateListPage(blogs);

        logger.info("所有静态页面生成完成");
    }

    /**
     * 生成单篇blog的详情页并上传至GridFS
     * 注意：不将readCount写入静态HTML，由Vue异步获取
     */
    private void generateDetailPage(Blog blog) throws Exception {
        // 构建静态化数据模型（排除readCount字段）
        Blog staticBlog = copyBlogWithoutReadCount(blog, true);

        Map<String, Object> data = new HashMap<>();
        data.put("blog", staticBlog);

        String html = freemarkerUtil.generateHtml("blogDetail.ftl", data);
        String fileName = "blog" + blog.getId() + ".html";

        gridFsUtil.uploadHtmlOverwrite(html, fileName);
        logger.info("详情页生成成功: {}", fileName);
    }

    /**
     * 生成博客列表页并上传至GridFS
     * 注意：不将readCount写入静态HTML，由Vue异步获取
     */
    private void generateListPage(List<Blog> blogs) throws Exception {
        // 构建静态化数据模型（排除readCount字段，列表不需要content）
        List<Blog> staticBlogs = new ArrayList<>();
        for (Blog blog : blogs) {
            staticBlogs.add(copyBlogWithoutReadCount(blog, false));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("blogs", staticBlogs);

        String html = freemarkerUtil.generateHtml("blogList.ftl", data);
        gridFsUtil.uploadHtmlOverwrite(html, "blogList.html");
        logger.info("列表页生成成功: blogList.html，包含{}篇blog", staticBlogs.size());
    }

    /**
     * 数据更新后的同步操作：重新生成对应详情页 + 重新生成列表页
     *
     * @param blogId 被更新的blog ID
     */
    public void syncAfterUpdate(Integer blogId) {
        if (blogId == null) return;
        try {
            Blog blog = jsonUtil.getBlogById(blogId);
            if (blog == null) {
                logger.warn("同步更新失败: 未找到blogId={}的数据", blogId);
                return;
            }
            // 重新生成并上传该blog的详情页（覆盖GridFS中的旧文件）
            generateDetailPage(blog);
            // 重新生成列表页（覆盖GridFS中的旧列表页）
            generateListPage(jsonUtil.getAllBlogs());
            logger.info("同步更新完成: blogId={}", blogId);
        } catch (Exception e) {
            logger.error("同步更新失败: blogId={}, error={}", blogId, e.getMessage(), e);
        }
    }

    /**
     * 数据新增后的同步操作：生成详情页 + 重新生成列表页
     *
     * @param blogId 新增的blog ID
     */
    public void syncAfterAdd(Integer blogId) {
        if (blogId == null) return;
        try {
            Blog blog = jsonUtil.getBlogById(blogId);
            if (blog == null) {
                logger.warn("同步新增失败: 未找到blogId={}的数据", blogId);
                return;
            }
            // 生成新blog的详情页并上传至GridFS
            generateDetailPage(blog);
            // 重新生成列表页
            generateListPage(jsonUtil.getAllBlogs());
            logger.info("同步新增完成: blogId={}", blogId);
        } catch (Exception e) {
            logger.error("同步新增失败: blogId={}, error={}", blogId, e.getMessage(), e);
        }
    }

    /**
     * 数据删除后的同步操作：删除对应详情页 + 重新生成列表页
     *
     * @param blogId 被删除的blog ID
     */
    public void syncAfterDelete(Integer blogId) {
        if (blogId == null) return;
        try {
            // 删除GridFS中对应的详情页
            String fileName = "blog" + blogId + ".html";
            gridFsUtil.deleteByFileName(fileName);
            logger.info("已删除详情页: {}", fileName);

            // 重新生成列表页
            List<Blog> remainingBlogs = jsonUtil.getAllBlogs();
            generateListPage(remainingBlogs);
            logger.info("同步删除完成: blogId={}, 剩余{}篇blog", blogId, remainingBlogs.size());
        } catch (Exception e) {
            logger.error("同步删除失败: blogId={}, error={}", blogId, e.getMessage(), e);
        }
    }

    /**
     * JSON文件外部修改回调：重新生成所有静态页面并同步到GridFS
     */
    public void onFileChanged() {
        try {
            logger.info("检测到JSON文件外部变更，开始全量重新生成静态页面...");
            generateAllPages();
            logger.info("文件变更同步完成");
        } catch (Exception e) {
            logger.error("文件变更同步失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 复制Blog对象，排除readCount字段（不参与静态化）
     *
     * @param source        源Blog对象
     * @param includeContent 是否包含content字段（列表页不需要）
     * @return 不含readCount的Blog副本
     */
    private Blog copyBlogWithoutReadCount(Blog source, boolean includeContent) {
        Blog copy = new Blog();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setAuthor(source.getAuthor());
        copy.setPublishTime(source.getPublishTime());
        copy.setCategory(source.getCategory());
        if (includeContent) {
            copy.setContent(source.getContent());
        }
        // readCount 不设置，不参与静态化，由Vue异步获取
        return copy;
    }
}
