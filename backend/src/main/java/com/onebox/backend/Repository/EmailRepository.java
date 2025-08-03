package com.onebox.backend.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onebox.backend.Model.Email;
import com.onebox.backend.Model.ImapAccount;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    Optional<Email> findByAccountAndMessageId(ImapAccount account, String messageId);
}