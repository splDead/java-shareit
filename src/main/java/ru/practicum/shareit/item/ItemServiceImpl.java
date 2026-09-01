package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto addItem(Long ownerId, ItemDto itemDto) {
        log.info("Добавление вещи пользователем {}", ownerId);

        if (!userRepository.existsById(ownerId)) {
            throw new NoSuchElementException("Пользователь с id " + ownerId + " не найден");
        }

        Item item = ItemMapper.toItem(itemDto);
        item.setOwnerId(ownerId);

        return ItemMapper.toItemDto(itemRepository.save(item));
    }


    @Override
    public ItemDto updateItem(Long ownerId, Long itemId, ItemDto itemDto) {
        log.info("Обновление вещи {} владельцем {}", itemId, ownerId);

        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new NoSuchElementException("Вещь с id " + itemId + " не найдена"));

        if (!item.getOwnerId().equals(ownerId)) {
            throw new NoSuchElementException("Пользователь с id " + ownerId + " не является владельцем вещи");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        return itemRepository.findById(itemId)
            .map(ItemMapper::toItemDto)
            .orElseThrow(() -> new NoSuchElementException("Вещь с id " + itemId + " не найдена"));
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long ownerId) {
        return itemRepository.findAllByOwnerId(ownerId).stream()
            .map(ItemMapper::toItemDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        return itemRepository.searchAvailableItems(text).stream()
            .map(ItemMapper::toItemDto)
            .collect(Collectors.toList());
    }
}
