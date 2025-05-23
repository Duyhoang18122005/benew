package com.example.backend.service;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public MessageServiceImpl(MessageRepository messageRepository, 
                            ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    @Transactional
    public Message sendMessage(User sender, User receiver, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        
        // Update or create conversation
        Conversation conversation = getOrCreateConversation(sender, receiver);
        conversation.setLastMessageContent(content);
        conversation.setLastMessageTime(message.getTimestamp());
        if (!sender.equals(conversation.getUser1())) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        conversationRepository.save(conversation);
        
        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversation(User user1, User user2) {
        return messageRepository.findConversation(user1, user2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(User user) {
        return messageRepository.findUnreadMessages(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(User user) {
        return messageRepository.countUnreadMessages(user);
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId) {
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setRead(true);
            messageRepository.save(message);
            
            // Update conversation unread count
            Conversation conversation = getOrCreateConversation(message.getSender(), message.getReceiver());
            if (conversation.getUnreadCount() > 0) {
                conversation.setUnreadCount(conversation.getUnreadCount() - 1);
                conversationRepository.save(conversation);
            }
        });
    }

    @Override
    @Transactional
    public void markAllMessagesAsRead(User user1, User user2) {
        List<Message> messages = messageRepository.findConversation(user1, user2);
        messages.forEach(message -> {
            if (message.getReceiver().equals(user1)) {
                message.setRead(true);
            }
        });
        messageRepository.saveAll(messages);
        
        // Reset conversation unread count
        Conversation conversation = getOrCreateConversation(user1, user2);
        conversation.setUnreadCount(0);
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findAllByUser(user);
    }

    @Override
    @Transactional
    public Conversation getOrCreateConversation(User user1, User user2) {
        Conversation conversation = conversationRepository.findConversationBetweenUsers(user1, user2);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUser1(user1);
            conversation.setUser2(user2);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation = conversationRepository.save(conversation);
        }
        return conversation;
    }
} 