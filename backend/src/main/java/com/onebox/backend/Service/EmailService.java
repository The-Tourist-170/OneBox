package com.onebox.backend.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onebox.backend.DTO.EmailDto;
import com.onebox.backend.Model.Email;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.Es.EmailEsRepository;
import com.onebox.backend.Repository.Jpa.EmailJpaRepository;
import com.onebox.backend.Repository.Jpa.ImapAccountRepository;
import com.onebox.backend.Repository.Jpa.UserRepository;
import com.onebox.backend.Utils.AESEncryptionUtil;

import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;

@Service
public class EmailService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImapAccountRepository imapAccountRepository;

    @Autowired
    private EmailJpaRepository emailRepository;

    @Autowired
    private EmailEsRepository emailESRepository;

    public void syncEmails(long accountId) {
        ImapAccount account = imapAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("IMAP Account not found"));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date sinceDate = cal.getTime();

        emailsFetch(account, "INBOX", sinceDate);
    }

    @Transactional
    public List<EmailDto> fetchEmails(long userId, String folderName, Date sinceDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        List<ImapAccount> accounts = imapAccountRepository.findByUser(user);
        List<EmailDto> emailDtoList = new ArrayList<>();

        for (ImapAccount account : accounts) {
            emailDtoList = emailsFetch(account, folderName, sinceDate);
        }

        return emailDtoList;
    }

    @Transactional
    public void processAndSaveMessage(Message message, ImapAccount account, String folder)
            throws MessagingException, IOException {
        String[] messageIdHeader = message.getHeader("Message-ID");
        String messageId = (messageIdHeader != null && messageIdHeader.length > 0) ? messageIdHeader[0] : null;

        if (messageId != null && emailRepository.findByAccountAndMessageId(account, messageId).isPresent()) {
            return;
        }

        Email emailEntity = convertToEmailEntity(message, account, folder, messageId);
        emailRepository.save(emailEntity);
        emailESRepository.save(emailEntity);
    }

    private List<EmailDto> emailsFetch(ImapAccount account, String folderName, Date sinceDate) {
        String password = AESEncryptionUtil.decrypt(account.getEncrypted_password());

        List<EmailDto> emailDtoList = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", account.getImap_server());
        props.put("mail.imaps.port", String.valueOf(account.getImap_port()));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(account.getEmailAddress(), password);
            }
        });

        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore();
            store.connect();

            String folderToOpen = (folderName != null && !folderName.isBlank()) ? folderName : "INBOX";
            folder = store.getFolder(folderToOpen);

            if (!folder.exists()) {
                throw new RuntimeException("Folder '" + folderToOpen + "' does not exist.");
            }

            folder.open(Folder.READ_ONLY);

            Message[] messages;

            if (sinceDate == null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -14);
                sinceDate = cal.getTime();
            }

            ReceivedDateTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, sinceDate);
            messages = folder.search(dateTerm);

            for (Message message : messages) {
                String[] messageIdHeader = message.getHeader("Message-ID");
                String messageId = (messageIdHeader != null && messageIdHeader.length > 0) ? messageIdHeader[0]
                        : null;

                boolean exists = (messageId != null)
                        && emailRepository.findByAccountAndMessageId(account, messageId).isPresent();

                if (exists) {
                    continue;
                }

                Email emailEntity = convertToEmailEntity(message, account, folderToOpen, messageId);

                emailRepository.save(emailEntity);
                emailESRepository.save(emailEntity);

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
        return emailDtoList;
    }

    private EmailDto convertToEmailDto(Message message) throws MessagingException, IOException {
        String subject = message.getSubject();

        String from = "";
        if (message.getFrom() != null && message.getFrom().length > 0) {
            from = message.getFrom()[0].toString();
        }

        Date sentDate = message.getSentDate();
        LocalDateTime sentLocalDateTime = null;

        if (sentDate != null) {
            sentLocalDateTime = LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault());
        } else {
            sentLocalDateTime = LocalDateTime.now();
        }

        String content = getTextFromMessage(message);

        return new EmailDto(subject, from, sentLocalDateTime, content);
    }

    private Email convertToEmailEntity(Message message, ImapAccount account, String folder, String messageId)
            throws MessagingException, IOException {
        Email email = new Email();

        email.setAccount(account);
        email.setAccountId(account.getId());

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
        LocalDateTime sentLocalDateTime = null;
        if (sentDate != null) {
            sentLocalDateTime = LocalDateTime.ofInstant(sentDate.toInstant(), ZoneOffset.UTC);
        } else {
            sentLocalDateTime = LocalDateTime.now(ZoneOffset.UTC);
        }

        email.setReceivedAt(sentLocalDateTime);
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
            } else if (bodyPart.getContent() instanceof Multipart) {
                String result = getTextFromMultipart((Multipart) bodyPart.getContent());
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return "";
    }
}