package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "contact_detail")
@ToString(exclude = "contact") 
public class ContactDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; 
    private String content;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id") 
    @JsonIgnore 
    private Contact contact;
}