package com.example.backend.repository;

import com.example.backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 继承 JpaRepository<实体类, 主键类型>
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    // 这里啥都不用写，自带 save, findAll, findById, delete
}