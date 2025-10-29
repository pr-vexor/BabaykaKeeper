package pr.vexor.telegrambot.babaykakeeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pr.vexor.telegrambot.babaykakeeper.model.Post;

@Repository
public interface ProcessedPostRepository extends JpaRepository<Post, Integer> {
    boolean existsByMessageId(Integer messageId);
}