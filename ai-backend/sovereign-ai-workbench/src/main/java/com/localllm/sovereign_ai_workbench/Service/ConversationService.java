package com.localllm.sovereign_ai_workbench.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.localllm.sovereign_ai_workbench.Entity.Conversation;
import com.localllm.sovereign_ai_workbench.Repo.ConversationRepository;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.User;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation createConversation(User user, String conversationName) {

        Conversation conversation = Conversation.builder()
                .conversationId(UUID.randomUUID().toString())
                .user(user)
                .conversationName(conversationName)
                .build();

        return conversationRepository.save(conversation);
    }

    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findByUser(user);
    }

    public Conversation getConversation(String conversationId, User user) {

        return conversationRepository
                .findByConversationIdAndUser(conversationId, user)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }
}