package com.billfang.util;

import com.billfang.entity.Blog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JSON文件解析工具类，负责blogData.json的读取、写入和文件变更监听
 */
@Component
public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** JSON文件路径（运行时解析） */
    private Path jsonFilePath;
    /** 内存缓存：当前blog数据集合 */
    private volatile List<Blog> blogCache = new CopyOnWriteArrayList<>();
    /** 上次内部写入的时间戳，用于抑制自身的文件变更事件 */
    private volatile long lastInternalWriteTime = 0;

    public JsonUtil(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 初始化：定位JSON文件、加载数据、启动文件监控
     */
    @PostConstruct
    public void init() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:blogData.json");
        File file = resource.getFile();
        this.jsonFilePath = file.toPath();
        loadFromFile();
        startFileWatcher();
        logger.info("JSON文件工具初始化完成，文件路径: {}", jsonFilePath);
    }

    /**
     * 获取所有blog数据
     */
    public List<Blog> getAllBlogs() {
        return new ArrayList<>(blogCache);
    }

    /**
     * 根据ID查询单个blog
     */
    public Blog getBlogById(Integer id) {
        if (id == null) return null;
        for (Blog blog : blogCache) {
            if (id.equals(blog.getId())) {
                return blog;
            }
        }
        return null;
    }

    /**
     * 更新blog数据并保存到JSON文件
     */
    public synchronized void updateBlog(Blog updatedBlog) throws IOException {
        if (updatedBlog == null || updatedBlog.getId() == null) {
            throw new IllegalArgumentException("blog对象或id不能为空");
        }
        boolean found = false;
        for (int i = 0; i < blogCache.size(); i++) {
            if (blogCache.get(i).getId().equals(updatedBlog.getId())) {
                blogCache.set(i, updatedBlog);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("未找到id为" + updatedBlog.getId() + "的blog");
        }
        saveToFile();
        logger.info("已更新blog: id={}, title={}", updatedBlog.getId(), updatedBlog.getTitle());
    }

    /**
     * 新增blog数据并保存到JSON文件，自动分配ID（当前最大ID+1）
     *
     * @param blog 新增的blog（id字段可为null，方法内自动分配）
     * @return 分配后的ID
     */
    public synchronized Integer addBlog(Blog blog) throws IOException {
        if (blog == null) {
            throw new IllegalArgumentException("blog对象不能为空");
        }
        // 自动分配ID：当前最大ID + 1，若无数据则从1开始
        int maxId = 0;
        for (Blog b : blogCache) {
            if (b.getId() != null && b.getId() > maxId) {
                maxId = b.getId();
            }
        }
        int newId = maxId + 1;
        blog.setId(newId);
        // 如果前端未提供readCount，默认设为0
        if (blog.getReadCount() == null) {
            blog.setReadCount(0);
        }
        blogCache.add(blog);
        saveToFile();
        logger.info("已新增blog: id={}, title={}", newId, blog.getTitle());
        return newId;
    }

    /**
     * 删除blog数据并保存到JSON文件
     */
    public synchronized void deleteBlog(Integer id) throws IOException {
        if (id == null) return;
        boolean removed = blogCache.removeIf(b -> id.equals(b.getId()));
        if (removed) {
            saveToFile();
            logger.info("已删除blog: id={}", id);
        }
    }

    /**
     * 递增指定blog的阅读数（每次+1）并持久化到JSON文件
     *
     * @param blogId blog ID
     * @return 递增后的阅读数
     */
    public synchronized int incrementReadCount(Integer blogId) throws IOException {
        Blog blog = getBlogById(blogId);
        if (blog == null) {
            throw new IllegalArgumentException("未找到id为" + blogId + "的blog");
        }
        int currentCount = blog.getReadCount() != null ? blog.getReadCount() : 0;
        blog.setReadCount(currentCount + 1);
        saveToFile();
        logger.debug("阅读数递增: blogId={}, readCount={}", blogId, blog.getReadCount());
        return blog.getReadCount();
    }

    /**
     * 注册数据变更监听器（当JSON文件被外部修改时触发）
     */
    public void registerChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * 从JSON文件加载数据到缓存
     */
    private synchronized void loadFromFile() throws IOException {
        File file = jsonFilePath.toFile();
        if (file.exists() && file.length() > 0) {
            List<Blog> loaded = objectMapper.readValue(file, new TypeReference<List<Blog>>() {});
            blogCache = new CopyOnWriteArrayList<>(loaded);
            logger.debug("已加载{}条blog数据", blogCache.size());
        } else {
            blogCache = new CopyOnWriteArrayList<>();
            logger.warn("blogData.json文件不存在或为空");
        }
    }

    /**
     * 将缓存数据保存到JSON文件，标记内部写入时间
     */
    private synchronized void saveToFile() throws IOException {
        lastInternalWriteTime = System.currentTimeMillis();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFilePath.toFile(), blogCache);
        logger.debug("已保存{}条blog数据到文件", blogCache.size());
    }

    /**
     * 启动文件变更监控线程（监控blogData.json的外部修改）
     */
    private void startFileWatcher() {
        Thread watcherThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                // 监控JSON文件所在目录
                Path dir = jsonFilePath.getParent();
                if (dir != null) {
                    dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                }

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException e) {
                        break;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changedFile = (Path) event.context();
                        if (jsonFilePath.getFileName().equals(changedFile)) {
                            // 延迟500ms避免文件写入未完成就读取
                            Thread.sleep(500);
                            // 如果是内部写入触发的变更（1秒内），跳过通知
                            if (System.currentTimeMillis() - lastInternalWriteTime < 1000) {
                                logger.debug("跳过内部写入触发的文件变更事件");
                            } else {
                                logger.info("检测到blogData.json外部修改，重新加载数据");
                                loadFromFile();
                                notifyListeners();
                            }
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                logger.error("文件监控异常: {}", e.getMessage(), e);
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.setName("JsonFileWatcher");
        watcherThread.start();
        logger.info("JSON文件监控线程已启动");
    }

    /**
     * 通知所有注册的变更监听器
     */
    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                logger.error("触发变更监听器异常: {}", e.getMessage(), e);
            }
        }
    }
}
