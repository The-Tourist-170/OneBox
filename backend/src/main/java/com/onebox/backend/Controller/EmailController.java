package com.onebox.backend.Controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.onebox.backend.DTO.EmailDto;
import com.onebox.backend.Service.EmailService;

@RestController
@RequestMapping("/emails")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchEmails(
            @RequestParam Long userId,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date sinceDate
    ) {
        try {
            List<EmailDto> emails = emailService.fetchEmails(userId, folder, sinceDate);
            return ResponseEntity.ok(emails);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
