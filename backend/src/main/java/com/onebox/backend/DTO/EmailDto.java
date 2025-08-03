package com.onebox.backend.DTO;

import java.util.Date;

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
    private Date sentDate;
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