package com.localllm.sovereign_ai_workbench.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.localllm.sovereign_ai_workbench.Entity.Conversation;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.User;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUser(User user);
    Optional<Conversation> findByConversationIdAndUser(String conversationId, User user);
}
