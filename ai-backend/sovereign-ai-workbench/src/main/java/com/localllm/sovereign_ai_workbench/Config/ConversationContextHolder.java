package com.localllm.sovereign_ai_workbench.Config;

import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import java.util.function.Consumer;

public class ConversationContextHolder {

    private static final ThreadLocal<String> CONVERSATION_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<AgentStreamEvent>> EVENT_LISTENER_HOLDER = new ThreadLocal<>();

    public static void setConversationId(String conversationId) {
        CONVERSATION_ID_HOLDER.set(conversationId);
    }

    public static String getConversationId() {
        return CONVERSATION_ID_HOLDER.get();
    }

    public static void setEventListener(Consumer<AgentStreamEvent> listener) {
        EVENT_LISTENER_HOLDER.set(listener);
    }

    public static Consumer<AgentStreamEvent> getEventListener() {
        return EVENT_LISTENER_HOLDER.get();
    }

    public static void emitEvent(AgentStreamEvent event) {
        Consumer<AgentStreamEvent> listener = EVENT_LISTENER_HOLDER.get();
        if (listener != null) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Ignore listener exceptions during streaming
            }
        }
    }

    public static void clear() {
        CONVERSATION_ID_HOLDER.remove();
        EVENT_LISTENER_HOLDER.remove();
    }
}
