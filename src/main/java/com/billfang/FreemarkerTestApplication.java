package com.billfang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot + Freemarker 页面静态化案例 - 主启动类
 *
 * 启动后自动将blog数据结合Freemarker模板生成静态HTML页面，
 * 上传至MongoDB GridFS存储，通过接口访问。
 * 阅读数等实时变动数据通过Vue异步获取。
 */
@SpringBootApplication
public class FreemarkerTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreemarkerTestApplication.class, args);
        System.out.println("========================================");
        System.out.println("  Freemarker 页面静态化服务启动完成");
        System.out.println("  访问博客列表: http://localhost:8080/getStaticPage/blogList.html");
        System.out.println("  访问博客详情: http://localhost:8080/getStaticPage/blog1.html");
        System.out.println("========================================");
    }
}
