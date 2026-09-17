package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.CreateGroup;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ItemDto addItem(@RequestHeader(USER_ID_HEADER) Long ownerId,
                           @Validated(CreateGroup.class) @RequestBody ItemDto itemDto) {
        log.info("Запрос POST /items от пользователя с id: {}", ownerId);
        return itemService.addItem(ownerId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto updateItem(@RequestHeader(USER_ID_HEADER) Long ownerId,
                              @PathVariable Long itemId,
                              @RequestBody ItemDto itemDto) {
        log.info("Запрос PATCH /items/{} от пользователя с id: {}", itemId, ownerId);
        return itemService.updateItem(ownerId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ItemDto getItemById(@RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId,
                               @PathVariable Long itemId) {
        log.info("Запрос GET /items/{} от пользователя id: {}", itemId, userId);
        return itemService.getItemById(itemId, userId);
    }

    @GetMapping
    public List<ItemDto> getItemsByOwner(@RequestHeader(USER_ID_HEADER) Long ownerId) {
        log.info("Запрос GET /items на получение всех вещей владельца с id: {}", ownerId);
        return itemService.getItemsByOwner(ownerId);
    }

    @GetMapping("/search")
    public List<ItemDto> searchItems(@RequestParam(defaultValue = "") String text) {
        log.info("Запрос GET /items/search со строкой поиска: {}", text);
        return itemService.searchItems(text);
    }

    @PostMapping("/{itemId}/comment")
    public CommentResponseDto addComment(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @PathVariable Long itemId,
                                         @Valid @RequestBody CommentRequestDto commentDto) {
        log.info("Запрос POST /items/{}/comment от пользователя id: {}", itemId, userId);
        return itemService.addComment(userId, itemId, commentDto);
    }
}
