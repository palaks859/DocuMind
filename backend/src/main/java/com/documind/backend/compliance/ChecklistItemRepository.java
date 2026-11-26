package com.documind.backend.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {
    List<ChecklistItem> findAllByChecklistIdOrderByItemOrderAsc(UUID checklistId);
    List<ChecklistItem> findAllByChecklistIdInOrderByItemOrderAsc(List<UUID> checklistIds);
}
