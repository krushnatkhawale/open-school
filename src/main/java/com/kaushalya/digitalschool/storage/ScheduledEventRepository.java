package com.kaushalya.digitalschool.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduledEventRepository extends JpaRepository<ScheduledEvent, UUID> {
    List<ScheduledEvent> findByEventDateOrderByEventTypeAsc(LocalDate eventDate);
    List<ScheduledEvent> findByEventDateBetweenOrderByEventDateAsc(LocalDate start, LocalDate end);
    List<ScheduledEvent> findByChatIdAndEventDateBetweenOrderByEventDateAsc(Long chatId, LocalDate start, LocalDate end);
    List<ScheduledEvent> findByChatIdInAndEventDateBetweenOrderByEventDateAsc(List<Long> chatIds, LocalDate start, LocalDate end);
}
