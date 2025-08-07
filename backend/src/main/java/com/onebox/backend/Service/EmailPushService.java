package com.onebox.backend.Service;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IdleManager;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Repository.Jpa.ImapAccountRepository;
import com.onebox.backend.Utils.AESEncryptionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.*;
import jakarta.mail.event.ConnectionAdapter;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class EmailPushService {

    private static final Logger logger = LoggerFactory.getLogger(EmailPushService.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<Long, IdleManager> idleManagerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Store> storeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, IMAPFolder> folderMap = new ConcurrentHashMap<>();

    @Autowired
    private EmailService emailService;

    @Autowired
    private ImapAccountRepository imapAccountRepository;

    @PostConstruct
    public void initializePushForAllAccounts() {
        logger.info("Initializing IMAP IDLE for all configured accounts...");
        List<ImapAccount> accounts = imapAccountRepository.findAll();
        for (ImapAccount account : accounts) {
            executorService.submit(() -> startPushForAccount(account));
        }
    }

    public void startPushForAccount(ImapAccount account) {
        Long accountId = account.getId();
        if (idleManagerMap.containsKey(accountId)) {
            logger.warn("Push service already running for accountId: {}. Skipping.", accountId);
            return;
        }

        logger.info("Starting push service for account: {}", account.getEmailAddress());

        try {
            String password = AESEncryptionUtil.decrypt(account.getEncrypted_password());

            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", account.getImap_server());
            props.put("mail.imaps.port", String.valueOf(account.getImap_port()));
            props.put("mail.imaps.starttls.enable", "true");
            props.put("mail.imaps.timeout", "10000");
            props.put("mail.imaps.connectiontimeout", "10000");
            props.put("mail.imaps.usesocketchannels", "true");

            Session session = Session.getInstance(props, null);
            Store store = session.getStore("imaps");

            store.addConnectionListener(new ConnectionAdapter() {
                @Override
                public void closed(ConnectionEvent e) {
                    logger.warn("Connection lost for accountId: {}. Attempting to reconnect...", accountId);
                    stopPushForAccount(accountId);
                    try {
                        Thread.sleep(15000); // 15-second delay
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    executorService.submit(() -> startPushForAccount(account));
                }
            });

            store.connect(account.getEmailAddress(), password);
            storeMap.put(accountId, store);

            final IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            if (!folder.exists()) {
                logger.error("INBOX folder not found for accountId: {}", accountId);
                return;
            }
            folder.open(Folder.READ_ONLY);
            folderMap.put(accountId, folder);

            folder.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent ev) {
                    logger.info("New messages arrived for accountId: {}. Count: {}", accountId,
                            ev.getMessages().length);
                    for (Message message : ev.getMessages()) {
                        try {
                            emailService.processAndSaveMessage(message, account, "INBOX");
                        } catch (MessagingException | IOException e) {
                            logger.error("Error processing new message for accountId {}: {}", accountId, e.getMessage(),
                                    e);
                        }
                    }
                }
            });

            IdleManager idleManager = new IdleManager(session, executorService);
            idleManager.watch(folder);
            idleManagerMap.put(accountId, idleManager);

            logger.info("Successfully started IMAP IDLE for accountId: {}. Watching INBOX.", accountId);

        } catch (MessagingException | IOException e) {
            logger.error("Failed to start push for accountId {}: {}. Check credentials or server settings.", accountId,
                    e.getMessage());
            stopPushForAccount(accountId);
        }
    }

    public void stopPushForAccount(Long accountId) {
        IdleManager idleManager = idleManagerMap.remove(accountId);
        if (idleManager != null) {
            idleManager.stop();
            logger.info("Stopped IdleManager for accountId: {}", accountId);
        }

        IMAPFolder folder = folderMap.remove(accountId);
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(false);
            } catch (MessagingException e) {
                logger.warn("Exception while closing folder for accountId {}: {}", accountId, e.getMessage());
            }
        }

        Store store = storeMap.remove(accountId);
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                logger.warn("Exception while closing store for accountId {}: {}", accountId, e.getMessage());
            }
        }
        logger.info("Cleaned up resources for accountId: {}", accountId);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down EmailPushService...");
        idleManagerMap.keySet().forEach(this::stopPushForAccount);

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("EmailPushService shut down complete.");
    }
}