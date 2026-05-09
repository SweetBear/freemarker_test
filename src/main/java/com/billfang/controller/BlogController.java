package com.billfang.controller;

import com.billfang.entity.Blog;
import com.billfang.service.StaticPageService;
import com.billfang.util.GridFsUtil;
import com.billfang.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blog相关REST接口控制器，提供阅读数查询、静态页面访问、数据增删改等接口
 */
@RestController
public class BlogController {

    private static final Logger logger = LoggerFactory.getLogger(BlogController.class);

    private final JsonUtil jsonUtil;
    private final GridFsUtil gridFsUtil;
    private final StaticPageService staticPageService;

    public BlogController(JsonUtil jsonUtil, GridFsUtil gridFsUtil, StaticPageService staticPageService) {
        this.jsonUtil = jsonUtil;
        this.gridFsUtil = gridFsUtil;
        this.staticPageService = staticPageService;
    }

    /**
     * 获取指定blog的实时阅读数（每次请求递增1并持久化到JSON文件）
     * Vue通过此接口异步获取阅读数并渲染
     *
     * @param blogId blog ID
     * @return 包含阅读数的JSON响应
     */
    @GetMapping("/getBlogReadCount/{blogId}")
    public Map<String, Object> getBlogReadCount(@PathVariable Integer blogId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 每次阅读递增1并持久化到JSON文件
            int readCount = jsonUtil.incrementReadCount(blogId);
            result.put("code", 200);
            result.put("data", readCount);
        } catch (IllegalArgumentException e) {
            result.put("code", 404);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("获取阅读数失败: blogId={}, error={}", blogId, e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "获取阅读数失败");
        }
        return result;
    }

    /**
     * 访问MongoDB GridFS中存储的静态页面
     * 浏览器通过此接口直接访问静态HTML页面
     *
     * @param pageName 页面文件名（如 blogList.html、blog1.html）
     * @return HTML页面内容
     */
    @GetMapping("/getStaticPage/{pageName}")
    public ResponseEntity<String> getStaticPage(@PathVariable String pageName) {
        try {
            // 校验页面名称格式，防止路径遍历攻击
            if (pageName == null || pageName.contains("/") || pageName.contains("\\")) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .body("<h1>请求错误</h1><p>页面名称格式不正确</p>");
            }

            String content = gridFsUtil.readFileContent(pageName);
            if (content == null) {
                logger.warn("页面不存在: {}", pageName);
                return ResponseEntity.status(404)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>404</title></head>" +
                              "<body style=\"font-family:Arial,sans-serif;text-align:center;padding:50px;\">" +
                              "<h1>404 - 页面不存在</h1>" +
                              "<p>未找到页面: " + pageName + "</p>" +
                              "<p><a href=\"/getStaticPage/blogList.html\">返回博客列表</a></p>" +
                              "</body></html>");
            }
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(content);
        } catch (Exception e) {
            logger.error("获取静态页面失败: pageName={}, error={}", pageName, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>500</title></head>" +
                          "<body style=\"font-family:Arial,sans-serif;text-align:center;padding:50px;\">" +
                          "<h1>500 - 服务器错误</h1>" +
                          "<p>页面访问异常: " + e.getMessage() + "</p>" +
                          "<p><a href=\"/getStaticPage/blogList.html\">返回博客列表</a></p>" +
                          "</body></html>");
        }
    }

    /**
     * 更新blog数据（通过接口修改），触发静态页面同步更新
     *
     * @param blog 更新后的blog数据
     * @return 操作结果
     */
    @PostMapping("/updateBlog")
    public Map<String, Object> updateBlog(@RequestBody Blog blog) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (blog == null || blog.getId() == null) {
                result.put("code", 400);
                result.put("message", "参数错误：blog对象或id不能为空");
                return result;
            }
            // 更新JSON文件中的数据
            jsonUtil.updateBlog(blog);
            // 同步更新GridFS中的静态页面
            staticPageService.syncAfterUpdate(blog.getId());
            result.put("code", 200);
            result.put("message", "更新成功，静态页面已同步");
            logger.info("通过接口更新blog成功: id={}", blog.getId());
        } catch (Exception e) {
            logger.error("更新blog失败: id={}, error={}", blog != null ? blog.getId() : "null", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 删除blog数据，触发GridFS中对应静态页面删除 + 列表页更新
     *
     * @param blogId blog ID
     * @return 操作结果
     */
    @DeleteMapping("/deleteBlog/{blogId}")
    public Map<String, Object> deleteBlog(@PathVariable Integer blogId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Blog blog = jsonUtil.getBlogById(blogId);
            if (blog == null) {
                result.put("code", 404);
                result.put("message", "博客不存在");
                return result;
            }
            // 从JSON文件中删除
            jsonUtil.deleteBlog(blogId);
            // 同步删除GridFS中的详情页并更新列表页
            staticPageService.syncAfterDelete(blogId);
            result.put("code", 200);
            result.put("message", "删除成功，静态页面已同步");
            logger.info("通过接口删除blog成功: id={}", blogId);
        } catch (Exception e) {
            logger.error("删除blog失败: id={}, error={}", blogId, e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 新增blog数据，保存到JSON文件，生成静态页面并上传至GridFS
     * ID自动分配（当前最大ID + 1），readCount默认设为0
     *
     * @param blog 新增的blog数据（id字段可不传，由后端自动分配）
     * @return 操作结果（包含分配的ID）
     */
    @PostMapping("/addBlog")
    public Map<String, Object> addBlog(@RequestBody Blog blog) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (blog == null) {
                result.put("code", 400);
                result.put("message", "参数错误：blog对象不能为空");
                return result;
            }
            // 自动分配ID并保存到JSON文件
            Integer newId = jsonUtil.addBlog(blog);
            // 生成详情页 + 重新生成列表页，上传至GridFS
            staticPageService.syncAfterAdd(newId);
            result.put("code", 200);
            result.put("message", "新增成功，静态页面已生成");
            result.put("data", newId);
            logger.info("通过接口新增blog成功: id={}, title={}", newId, blog.getTitle());
        } catch (Exception e) {
            logger.error("新增blog失败: title={}, error={}", blog != null ? blog.getTitle() : "null", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "新增失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有blog数据（JSON格式，供调试和前端参考）
     *
     * @return blog列表JSON
     */
    @GetMapping("/listBlogs")
    public Map<String, Object> listBlogs() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Blog> blogs = jsonUtil.getAllBlogs();
            result.put("code", 200);
            result.put("data", blogs);
        } catch (Exception e) {
            logger.error("获取blog列表失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}
