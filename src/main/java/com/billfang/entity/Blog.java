package com.billfang.entity;

/**
 * Blog文章实体类，对应blogData.json中的数据结构
 */
public class Blog {

    /** 文章ID */
    private Integer id;
    /** 文章标题 */
    private String title;
    /** 作者 */
    private String author;
    /** 发布时间 */
    private String publishTime;
    /** 分类 */
    private String category;
    /** 文章内容 */
    private String content;
    /** 阅读数（实时变动数据，不参与静态化） */
    private Integer readCount;

    public Blog() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    @Override
    public String toString() {
        return "Blog{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publishTime='" + publishTime + '\'' +
                ", category='" + category + '\'' +
                ", readCount=" + readCount +
                '}';
    }
}
