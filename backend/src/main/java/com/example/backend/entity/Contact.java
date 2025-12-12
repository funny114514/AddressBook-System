package com.example.backend.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "contact")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ExcelProperty(value = "ID", index = 0)
    private Long id;

    @ExcelProperty(value = "姓名", index = 1)
    private String name;

    @ExcelProperty(value = "地址", index = 2)
    private String address;

    @Column(name = "is_favorite")
    @ExcelIgnore
    private Boolean isFavorite = false;
    @Column(name = "created_at", updatable = false) 
    @CreationTimestamp 
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 
    @ExcelIgnore 
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @ExcelIgnore
    private List<ContactDetail> details = new ArrayList<>();
}