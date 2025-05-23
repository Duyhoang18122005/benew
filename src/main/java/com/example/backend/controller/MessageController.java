package com.example.backend.controller;

import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.service.MessageService;
import com.example.backend.service.UserService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:3000")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    public MessageController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @PostMapping("/send/{receiverId}")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long receiverId,
            @RequestBody MessageRequest request,
            Authentication authentication) {
        User sender = userService.findByUsername(authentication.getName());
        User receiver = userService.findById(receiverId);
        
        Message message = messageService.sendMessage(sender, receiver, request.getContent());
        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long userId,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        User otherUser = userService.findById(userId);
        
        List<Message> conversation = messageService.getConversation(currentUser, otherUser);
        return ResponseEntity.ok(conversation);
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Message> unreadMessages = messageService.getUnreadMessages(user);
        return ResponseEntity.ok(unreadMessages);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadMessageCount(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        long count = messageService.getUnreadMessageCount(user);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/read/{messageId}")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long messageId) {
        messageService.markMessageAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all/{userId}")
    public ResponseEntity<?> markAllMessagesAsRead(
            @PathVariable Long userId,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        User otherUser = userService.findById(userId);
        
        messageService.markAllMessagesAsRead(currentUser, otherUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(messageService.getUserConversations(user));
    }
}

@Data
class MessageRequest {
    private String content;
} 