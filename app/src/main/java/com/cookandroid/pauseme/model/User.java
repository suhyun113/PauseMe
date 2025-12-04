package com.cookandroid.pauseme.model;

public class User {
    public String uid;
    public String email;
    public String userId;   // 로그인용 아이디
    public String nickname;

    public User() {
        // Firebase 직렬화용 기본 생성자
    }

    public User(String uid, String email, String userId, String nickname) {
        this.uid = uid;
        this.email = email;
        this.userId = userId;
        this.nickname = nickname;
    }
}
