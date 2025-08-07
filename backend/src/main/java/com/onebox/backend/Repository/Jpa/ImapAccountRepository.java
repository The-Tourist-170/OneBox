package com.onebox.backend.Repository.Jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Model.User;

@Repository
public interface ImapAccountRepository extends JpaRepository<ImapAccount, Long>{
    Optional<ImapAccount> findByUserAndEmailAddress(User user, String email);

    List<ImapAccount> findByUser(User user);
}