package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import java.util.List;

public interface ItemService {
    ItemDto addItem(Long ownerId, ItemDto itemDto);
    ItemDto updateItem(Long ownerId, Long itemId, ItemDto itemDto);
    ItemDto getItemById(Long itemId);
    List<ItemDto> getItemsByOwner(Long ownerId);
    List<ItemDto> searchItems(String text);
}
