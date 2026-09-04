package com.localllm.sovereign_ai_workbench.Entity;

import com.localllm.sovereign_ai_workbench.jwtauth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    private String conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String conversationName;

    public Conversation() {
    }

    public Conversation(String conversationId, User user, String conversationName) {
        this.conversationId = conversationId;
        this.user = user;
        this.conversationName = conversationName;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public static ConversationBuilder builder() {
        return new ConversationBuilder();
    }

    public static class ConversationBuilder {
        private String conversationId;
        private User user;
        private String conversationName;

        public ConversationBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public ConversationBuilder user(User user) {
            this.user = user;
            return this;
        }

        public ConversationBuilder conversationName(String conversationName) {
            this.conversationName = conversationName;
            return this;
        }

        public Conversation build() {
            return new Conversation(conversationId, user, conversationName);
        }
    }
}