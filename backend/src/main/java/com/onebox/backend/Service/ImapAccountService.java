package com.onebox.backend.Service;

import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onebox.backend.DTO.ImapAccountDto;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.ImapAccountRepository;
import com.onebox.backend.Repository.UserRepository;
import com.onebox.backend.Utils.AESEncryptionUtil;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

@Service
public class ImapAccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImapAccountRepository imapAccountRepository;

    public ImapAccount addImapAccount(ImapAccountDto dto) {
        Optional<User> optionalUser = userRepository.findById(dto.getUserId());
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found");
        }
        User user = optionalUser.get();

        if (imapAccountExists(user, dto.getEmail())) {
            throw new RuntimeException("Account already connected!");
        }

        if (!testImapConnection(dto.getEmail(), dto.getImapServer(), dto.getImapPort(), dto.getPassword())) {
            throw new RuntimeException("Invalid IMAP credentials");
        }

        String encryptedPassword = AESEncryptionUtil.encrypt(dto.getPassword());

        ImapAccount newImapAccount = new ImapAccount();
        newImapAccount.setUser(user);
        newImapAccount.setEmailAddress(dto.getEmail());
        newImapAccount.setImap_server(dto.getImapServer());
        newImapAccount.setImap_port(dto.getImapPort());
        newImapAccount.setEncrypted_password(encryptedPassword);

        return imapAccountRepository.save(newImapAccount);
    }

    public boolean imapAccountExists(User user, String email) {
        return imapAccountRepository.findByUserAndEmailAddress(user, email).isPresent();
    }

    public boolean testImapConnection(String email, String imapServer, int imapPort, String password) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", imapServer);
        props.put("mail.imap.port", String.valueOf(imapPort));
        props.put("mail.imap.ssl.enable", "true");

        Session session = Session.getInstance(props);

        try {
            Store store = session.getStore("imap");
            store.connect(imapServer, imapPort, email, password);
            store.close();
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }
}