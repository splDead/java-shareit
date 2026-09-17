package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
public class ItemRequestController {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ItemRequestDto createRequest(@RequestHeader(USER_ID_HEADER) Long userId,
                                        @RequestBody ItemRequestDto itemRequestDto) {
        log.info("Запрос POST /requests на добавление запроса от пользователя: {}", userId);

        itemRequestDto.setRequestorId(userId);
        itemRequestDto.setCreated(LocalDateTime.now());

        return itemRequestDto;
    }

    @GetMapping
    public List<ItemRequestDto> getOwnRequests(@RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Запрос GET /requests от пользователя: {} для получения своих запросов", userId);

        return List.of();
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getAllRequests(@RequestHeader(USER_ID_HEADER) Long userId,
                                               @RequestParam(defaultValue = "0") int from,
                                               @RequestParam(defaultValue = "10") int size) {
        log.info("Запрос GET /requests/all от пользователя: {} с пагинацией from={}, size={}", userId, from, size);

        return List.of();
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getRequestById(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @PathVariable Long requestId) {
        log.info("Запрос GET /requests/{} от пользователя: {}", requestId, userId);

        return ItemRequestDto.builder().id(requestId).build();
    }
}
