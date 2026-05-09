package com.billfang.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.StringWriter;
import java.util.Map;

/**
 * Freemarker静态化工具类，负责将模板与数据结合生成静态HTML内容，
 * 不包含readCount字段（由Vue异步获取）
 */
@Component
public class FreemarkerUtil {

    private static final Logger logger = LoggerFactory.getLogger(FreemarkerUtil.class);

    private final FreeMarkerConfigurer freemarkerConfigurer;

    public FreemarkerUtil(FreeMarkerConfigurer freemarkerConfigurer) {
        this.freemarkerConfigurer = freemarkerConfigurer;
    }

    /**
     * 根据模板和数据生成静态HTML内容
     *
     * @param templateName 模板文件名（如 blogList.ftl、blogDetail.ftl）
     * @param dataModel    数据模型（不包含readCount等实时变动字段）
     * @return 生成的HTML字符串
     */
    public String generateHtml(String templateName, Map<String, Object> dataModel) {
        try {
            Configuration config = freemarkerConfigurer.getConfiguration();
            Template template = config.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            String html = writer.toString();
            logger.info("模板渲染成功: template={}, size={}bytes", templateName, html.length());
            return html;
        } catch (Exception e) {
            logger.error("模板渲染失败: template={}, error={}", templateName, e.getMessage(), e);
            throw new RuntimeException("Freemarker模板渲染失败: " + templateName, e);
        }
    }
}
