package com.onebox.backend.Model;

import java.time.LocalDateTime;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "emails")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(indexName = "emails")
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Field(type = FieldType.Long)
    private long id;

    @Transient 
    @Field(type = FieldType.Long) 
    private Long accountId;

    @Column(name = "message_id", unique = true)
    @Field(type = FieldType.Keyword)
    private String messageId;

    @ManyToOne
    @JoinColumn(name = "account_id")
        @Field(type = FieldType.Object)
    private ImapAccount account;

    @Field(type = FieldType.Text)
    private String subject;

    @Field(type = FieldType.Text)
    private String sender;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Field(type = FieldType.Text)
    private String body;

    @Field(type = FieldType.Keyword)
    private String folder;

    @Field(type = FieldType.Keyword)
    private String label;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime receivedAt;
}