package com.localllm.sovereign_ai_workbench.Repo;

import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    List<Artifact> findByConversationId(String conversationId);

    Optional<Artifact> findByConversationIdAndFilePath(String conversationId, String filePath);

    void deleteByConversationIdAndFilePath(String conversationId, String filePath);
}
