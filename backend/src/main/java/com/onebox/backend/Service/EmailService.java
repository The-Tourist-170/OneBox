package com.onebox.backend.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onebox.backend.DTO.EmailDto;
import com.onebox.backend.Model.Email;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.EmailRepository;
import com.onebox.backend.Repository.ImapAccountRepository;
import com.onebox.backend.Repository.UserRepository;
import com.onebox.backend.Utils.AESEncryptionUtil;

import jakarta.mail.*;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.transaction.Transactional;

@Service
public class EmailService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImapAccountRepository imapAccountRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Transactional
    public List<EmailDto> fetchEmails(long userId, String folderName, Date sinceDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        List<ImapAccount> accounts = imapAccountRepository.findByUser(user);
        List<EmailDto> emailDtoList = new ArrayList<>();

        for (ImapAccount imapAccount : accounts) {
            String password = AESEncryptionUtil.decrypt(imapAccount.getEncrypted_password());

            Properties props = new Properties();
            props.put("mail.store.protocol", "imap");
            props.put("mail.imap.host", imapAccount.getImap_server());
            props.put("mail.imap.port", String.valueOf(imapAccount.getImap_port()));
            props.put("mail.imap.ssl.enable", "true");

            Session session = Session.getInstance(props);

            Store store = null;
            Folder folder = null;
            try {
                store = session.getStore("imap");
                store.connect(imapAccount.getImap_server(), imapAccount.getImap_port(),
                        imapAccount.getEmailAddress(), password);

                String folderToOpen = (folderName != null && !folderName.isBlank()) ? folderName : "INBOX";
                folder = store.getFolder(folderToOpen);

                if (!folder.exists()) {
                    throw new RuntimeException("Folder '" + folderToOpen + "' does not exist.");
                }

                folder.open(Folder.READ_ONLY);

                Message[] messages;
                if (sinceDate != null) {
                    ReceivedDateTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GT, sinceDate);
                    messages = folder.search(dateTerm);
                } else {
                    messages = folder.getMessages();
                }

                for (Message message : messages) {
                    String[] messageIdHeader = message.getHeader("Message-ID");
                    String messageId = (messageIdHeader != null && messageIdHeader.length > 0) ? messageIdHeader[0] : null;

                    boolean exists = (messageId != null) && emailRepository.findByAccountAndMessageId(imapAccount, messageId).isPresent();

                    if (exists) {
                        continue;
                    }

                    Email emailEntity = convertToEmailEntity(message, imapAccount, folderToOpen, messageId);

                    emailRepository.save(emailEntity);

                    emailDtoList.add(convertToEmailDto(message));
                }

            } catch (MessagingException | IOException e) {
                throw new RuntimeException("Error fetching emails: " + e.getMessage(), e);
            } finally {
                try {
                    if (folder != null && folder.isOpen()) {
                        folder.close(false);
                    }
                    if (store != null && store.isConnected()) {
                        store.close();
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }

        return emailDtoList;
    }

    private EmailDto convertToEmailDto(Message message) throws MessagingException, IOException {
        String subject = message.getSubject();

        String from = "";
        if (message.getFrom() != null && message.getFrom().length > 0) {
            from = message.getFrom()[0].toString();
        }

        Date sentDate = message.getSentDate();
        String content = getTextFromMessage(message);

        return new EmailDto(subject, from, sentDate, content);
    }

    private Email convertToEmailEntity(Message message, ImapAccount account, String folder, String messageId) throws MessagingException, IOException {
        Email email = new Email();

        email.setAccount(account);
        email.setFolder(folder);
        email.setMessageId(messageId);

        email.setSubject(message.getSubject());

        String from = "";
        if (message.getFrom() != null && message.getFrom().length > 0) {
            from = message.getFrom()[0].toString();
        }
        email.setSender(from);

        String content = getTextFromMessage(message);
        email.setBody(content);

        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            LocalDateTime receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(sentDate.getTime()), ZoneId.systemDefault());
            email.setReceivedAt(receivedAt);
        } else {
            email.setReceivedAt(LocalDateTime.now());
        }

        email.setLabel(null); 

        return email;
    }

    private String getTextFromMessage(Message message) throws IOException, MessagingException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            return getTextFromMultipart(multipart);
        } else if (message.isMimeType("text/html")) {
            return "";  
        }
        return "";
    }

    private String getTextFromMultipart(Multipart multipart) throws IOException, MessagingException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return bodyPart.getContent().toString();
            }
            else if (bodyPart.getContent() instanceof Multipart) {
                String result = getTextFromMultipart((Multipart) bodyPart.getContent());
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return "";
    }
}