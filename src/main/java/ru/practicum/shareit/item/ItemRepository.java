package ru.practicum.shareit.item;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ItemRepository {
    private final Map<Long, Item> items = new HashMap<>();
    private long currentId = 1;

    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(currentId++);
        }

        items.put(item.getId(), item);

        return item;
    }

    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<Item> findAllByOwnerId(Long ownerId) {
        return items.values().stream()
            .filter(item -> item.getOwnerId() != null && item.getOwnerId().equals(ownerId)) // Исправлено на getOwnerId()
            .collect(Collectors.toList());
    }

    public List<Item> searchAvailableItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();

        return items.values().stream()
            .filter(item -> item.getAvailable() != null && item.getAvailable())
            .filter(item -> (item.getName() != null && item.getName().toLowerCase().contains(lowerText))
                    || (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerText)))
            .collect(Collectors.toList());
    }
}
