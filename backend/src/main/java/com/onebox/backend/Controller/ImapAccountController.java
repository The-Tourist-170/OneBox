package com.onebox.backend.Controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.onebox.backend.DTO.ImapAccountDto;
import com.onebox.backend.Model.ImapAccount;
import com.onebox.backend.Model.User;
import com.onebox.backend.Service.ImapAccountService;
import com.onebox.backend.Service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
@Validated
public class ImapAccountController {

    @Autowired
    private ImapAccountService imapAccountService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> addImapAccount(@Valid @RequestBody ImapAccountDto dto) throws IOException {
        try {
            User currentUser = getCurrentUser();
            dto.setUserId(currentUser.getId()); 

            ImapAccount account = imapAccountService.addImapAccount(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("id", account.getId());
            response.put("message", "IMAP Account connected successfully: " + account.getEmailAddress());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getAccountsByUser() {
        User currentUser = getCurrentUser();
        List<ImapAccount> accounts = imapAccountService.getAccountsByUser(currentUser.getId());
        return ResponseEntity.ok(accounts);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long accountId) {
        User currentUser = getCurrentUser();
        imapAccountService.deleteAccount(accountId, currentUser.getId());
        return ResponseEntity.ok("IMAP Account deleted");
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        String email;

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        } else {
            throw new RuntimeException("Unauthorized - unable to get user from security context");
        }
        return userService.findByEmail(email);
    }
}