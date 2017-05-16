package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.MomentStatus;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class Comment extends RealmObject {
    @PrimaryKey
    private String id;
    private String momentId;
    private long createTime;
    private String nickname;
    private String avatar;
    private String content;
    private long lastVisitTime;

    public String getId() {
        return id;
    }

    public String getMomentId() {
        return momentId;
    }

    public void setMomentId(String momentId) {
        this.momentId = momentId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }
}