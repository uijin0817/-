package com.example.smiti;

public class Post {
    private String id;
    private String title;
    private String content;
    private String author; // 또는 email 등

    // 기본 생성자 추가
    public Post() {
    }

    // 기존 생성자들 (유지)
    public Post(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public Post(String id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    // Getter 및 Setter 메서드들 (유지)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}