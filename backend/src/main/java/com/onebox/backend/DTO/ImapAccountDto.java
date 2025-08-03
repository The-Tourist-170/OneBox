package com.onebox.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ImapAccountDto {
    private Long userId;
    private String email;
    private String imapServer;
    private int imapPort;
    private String password;
}