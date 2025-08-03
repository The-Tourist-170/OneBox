package com.onebox.backend.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.onebox.backend.DTO.ImapAccountDto;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Repository.ImapAccountRepository;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.UserRepository;
import com.onebox.backend.Service.ImapAccountService;

@RestController
@RequestMapping("/accounts")
public class ImapAccountController {

    @Autowired
    private ImapAccountService imapAccountService;  

    @Autowired
    private ImapAccountRepository imapAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> addImapAccount(@RequestBody ImapAccountDto dto) {
        try {
            ImapAccount account = imapAccountService.addImapAccount(dto);
            return ResponseEntity.ok("IMAP Account connected successfully: " + account.getEmailAddress());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAccountsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User does not exist");
        }
        List<ImapAccount> accounts = imapAccountRepository.findByUser(user);
        return ResponseEntity.ok(accounts);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long accountId) {
        if (!imapAccountRepository.existsById(accountId)) {
            return ResponseEntity.badRequest().body("IMAP Account does not exist");
        }
        imapAccountRepository.deleteById(accountId);
        return ResponseEntity.ok("IMAP Account deleted");
    }
}
