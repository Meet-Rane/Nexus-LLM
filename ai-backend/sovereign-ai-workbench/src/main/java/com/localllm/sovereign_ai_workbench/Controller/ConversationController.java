package com.localllm.sovereign_ai_workbench.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.localllm.sovereign_ai_workbench.Dto.ConversationDto;
import com.localllm.sovereign_ai_workbench.Entity.Conversation;
import com.localllm.sovereign_ai_workbench.Service.ConversationService;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.User;
import com.localllm.sovereign_ai_workbench.jwtauth.repository.UserRepository;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(
            ConversationService conversationService,
            UserRepository userRepository) {

        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
            @RequestParam String conversationName,
            Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation =
                conversationService.createConversation(user, conversationName);

        ConversationDto convers = new ConversationDto(
                conversation.getConversationId(),
                conversation.getConversationName()
        );

        return ResponseEntity.ok(convers);
    }

    @GetMapping
    public ResponseEntity<List<ConversationDto>> getUserConversations(
            Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ConversationDto> conversations = conversationService
                .getUserConversations(user)
                .stream()
                .map(conversation -> new ConversationDto(
                        conversation.getConversationId(),
                        conversation.getConversationName()
                ))
                .toList();

        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<Conversation> getConversation(
            @PathVariable String conversationId,
            Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                conversationService.getConversation(conversationId, user)
        );
    }
}