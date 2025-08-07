package com.onebox.backend.Repository.Jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onebox.backend.Model.Email;
import com.onebox.backend.Model.ImapAccount;

@Repository
public interface EmailJpaRepository extends JpaRepository<Email, Long> {
    Optional<Email> findByAccountAndMessageId(ImapAccount account, String messageId);

    void deleteAllByAccount_Id(Long accountId);
}