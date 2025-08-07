package com.onebox.backend.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.onebox.backend.DTO.ImapAccountDto;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Model.ImapAccount.Provider;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.Es.EmailEsRepository;
import com.onebox.backend.Repository.Jpa.EmailJpaRepository;
import com.onebox.backend.Repository.Jpa.ImapAccountRepository;
import com.onebox.backend.Repository.Jpa.UserRepository;
import com.onebox.backend.Utils.AESEncryptionUtil;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.transaction.Transactional;

@Service
public class ImapAccountService {

    private static final Logger logger = LoggerFactory.getLogger(ImapAccountService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailJpaRepository emailJpaRepository;

    @Autowired
    private EmailEsRepository emailEsRepository;

    @Autowired
    private ImapAccountRepository imapAccountRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailPushService emailPushService;

    private final ConcurrentHashMap<Long, Store> storeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Session> sessionCache = new ConcurrentHashMap<>();

    public ImapAccount addImapAccount(ImapAccountDto dto) throws IOException {
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
        newImapAccount.setProvider(detectProvider(dto.getEmail())); // Auto-detect
        newImapAccount.setImap_server(dto.getImapServer());
        newImapAccount.setImap_port(dto.getImapPort());
        newImapAccount.setEncrypted_password(encryptedPassword);

        ImapAccount savedAccount = imapAccountRepository.save(newImapAccount);

        emailService.syncEmails(savedAccount.getId());
        getImapStore(savedAccount);
        emailPushService.startPushForAccount(savedAccount);
        return savedAccount;
    }

    private Provider detectProvider(String email) {
        if (email.endsWith("@gmail.com"))
            return Provider.GMAIL;
        if (email.endsWith("@outlook.com"))
            return Provider.OUTLOOK;
        if (email.endsWith("@yahoo.com"))
            return Provider.YAHOO;
        return Provider.CUSTOM;
    }

    public boolean imapAccountExists(User user, String email) {
        return imapAccountRepository.findByUserAndEmailAddress(user, email).isPresent();
    }

    public boolean testImapConnection(String email, String imapServer, int imapPort, String password) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapServer);
        props.put("mail.imaps.port", String.valueOf(imapPort));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        try {
            Store store = session.getStore();
            store.connect();
            store.close();
            return true;
        } catch (MessagingException e) {
            logger.error("IMAP test connection failed: {}", e.getMessage());
            return false;
        }
    }

    public Store getImapStore(ImapAccount account) {
        Long accountId = account.getId();
        return storeCache.computeIfAbsent(accountId, k -> {
            String decryptedPassword = AESEncryptionUtil.decrypt(account.getEncrypted_password());
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", account.getImap_server());
            props.put("mail.imaps.port", String.valueOf(account.getImap_port()));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(account.getEmailAddress(), decryptedPassword);
                }
            });

            sessionCache.put(accountId, session);

            try {
                Store store = session.getStore();
                store.connect();
                logger.info("IMAP store connected for accountId: {}", accountId);
                return store;
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to connect IMAP store: " + e.getMessage());
            }
        });
    }

    public Session getSession(Long accountId) {
        Session session = sessionCache.get(accountId);
        if (session == null) {
            throw new RuntimeException("Session not found for accountId: " + accountId);
        }
        return session;
    }

    public List<ImapAccount> getAccountsByUser(long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found with id: " + id);
        }
        User user = optionalUser.get();
        return imapAccountRepository.findByUser(user);
    }

    @Transactional
    public void deleteAccount(Long accountId, long userId) {
        Optional<ImapAccount> optionalAccount = imapAccountRepository.findById(accountId);
        if (optionalAccount.isEmpty()) {
            throw new RuntimeException("IMAP Account not found with id: " + accountId);
        }
        ImapAccount account = optionalAccount.get();
        if (account.getUser().getId() != userId) {
            throw new RuntimeException("Unauthorized: Account does not belong to this user");
        }
        emailPushService.stopPushForAccount(accountId);
        emailJpaRepository.deleteAllByAccount_Id(accountId);
        emailEsRepository.deleteAllByAccountId(accountId);

        Store store = storeCache.remove(accountId);
        if (store != null) {
            try {
                store.close();
                logger.info("IMAP store closed for accountId: {}", accountId);
            } catch (MessagingException e) {
                logger.warn("Failed to close IMAP store: {}", e.getMessage());
            }
        }
        sessionCache.remove(accountId);
        imapAccountRepository.delete(account);
    }
}