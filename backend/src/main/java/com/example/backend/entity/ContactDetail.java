package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "contact_detail")
@ToString(exclude = "contact") // 防止打印日志死循环
public class ContactDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // 手机/微信
    private String content; // 号码

    // ============ 核心修改：明确指定所属的联系人 ============
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id") // 对应数据库的外键列
    @JsonIgnore // 极其重要！防止导出JSON时死循环（爸爸找儿子，儿子找爸爸...）
    private Contact contact;
}