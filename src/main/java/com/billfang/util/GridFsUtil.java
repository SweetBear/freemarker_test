package com.billfang.util;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * MongoDB GridFS工具类，提供静态页面文件的上传、删除、查询、读取操作
 */
@Component
public class GridFsUtil {

    private static final Logger logger = LoggerFactory.getLogger(GridFsUtil.class);

    /** GridFS中存储HTML文件的Content-Type */
    private static final String HTML_CONTENT_TYPE = "text/html";

    private final GridFsTemplate gridFsTemplate;

    public GridFsUtil(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    /**
     * 将HTML内容上传到GridFS（如果同名文件已存在则先删除再上传）
     *
     * @param htmlContent HTML字符串内容
     * @param fileName    文件名（如 blog1.html、blogList.html）
     * @return GridFS文件ID
     */
    public String uploadHtmlOverwrite(String htmlContent, String fileName) {
        // 先删除已存在的同名文件，避免GridFS中出现重复文件
        deleteByFileName(fileName);
        return uploadHtml(htmlContent, fileName);
    }

    /**
     * 将HTML内容上传到GridFS
     *
     * @param htmlContent HTML字符串内容
     * @param fileName    文件名
     * @return GridFS文件ID
     */
    public String uploadHtml(String htmlContent, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
            String fileId = gridFsTemplate.store(inputStream, fileName, HTML_CONTENT_TYPE).toString();
            logger.info("文件上传成功: fileName={}, fileId={}", fileName, fileId);
            return fileId;
        } catch (Exception e) {
            logger.error("文件上传失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new RuntimeException("GridFS文件上传失败: " + fileName, e);
        }
    }

    /**
     * 根据文件名删除GridFS中的文件
     *
     * @param fileName 文件名
     */
    public void deleteByFileName(String fileName) {
        try {
            Query query = new Query(Criteria.where("filename").is(fileName));
            gridFsTemplate.delete(query);
            logger.info("文件删除成功: fileName={}", fileName);
        } catch (Exception e) {
            logger.error("文件删除失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new RuntimeException("GridFS文件删除失败: " + fileName, e);
        }
    }

    /**
     * 根据文件名查询GridFS文件
     *
     * @param fileName 文件名
     * @return GridFSFile对象，不存在则返回null
     */
    public GridFSFile findByFileName(String fileName) {
        Query query = new Query(Criteria.where("filename").is(fileName));
        return gridFsTemplate.findOne(query);
    }

    /**
     * 检查指定文件名的文件是否存在于GridFS中
     *
     * @param fileName 文件名
     * @return true存在，false不存在
     */
    public boolean fileExists(String fileName) {
        return findByFileName(fileName) != null;
    }

    /**
     * 根据文件名读取GridFS文件内容（HTML字符串）
     *
     * @param fileName 文件名
     * @return 文件内容字符串，文件不存在或读取失败返回null
     */
    public String readFileContent(String fileName) {
        try {
            GridFSFile file = findByFileName(fileName);
            if (file == null) {
                logger.warn("文件不存在: fileName={}", fileName);
                return null;
            }
            GridFsResource resource = gridFsTemplate.getResource(file);
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            logger.debug("文件读取成功: fileName={}, size={}bytes", fileName, content.length());
            return content;
        } catch (Exception e) {
            logger.error("文件读取失败: fileName={}, error={}", fileName, e.getMessage(), e);
            return null;
        }
    }
}
