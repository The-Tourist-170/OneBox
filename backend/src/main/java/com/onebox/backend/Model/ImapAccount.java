package com.onebox.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "imap_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "emailAddress"})
)
public class ImapAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String emailAddress;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    private String imap_server;

    private int imap_port;

    private String encrypted_password;

    public enum Provider {
        GMAIL,
        OUTLOOK,
        YAHOO,
        CUSTOM
    }
}