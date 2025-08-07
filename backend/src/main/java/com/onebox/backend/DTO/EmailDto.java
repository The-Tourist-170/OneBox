package com.onebox.backend.DTO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmailDto {
    private String subject;
    private String from;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentDate;

    private String content;

    @Override
    public String toString() {
        return "EmailMessage{" +
                "subject='" + subject + '\'' +
                ", from='" + from + '\'' +
                ", sentDate=" + sentDate +
                ", content='" + content + '\'' +
                '}';
    }
}