package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
public class ItemRequestController {

    private final ItemRequestClient itemRequestClient;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ResponseEntity<Object> createRequest(@RequestHeader(USER_ID_HEADER) long userId,
                                                @Valid @RequestBody ItemRequestDto itemRequestDto) {
        log.info("Запрос POST /requests от пользователя {}", userId);
        return itemRequestClient.createRequest(userId, itemRequestDto);
    }

    @GetMapping
    public ResponseEntity<Object> getOwnRequests(@RequestHeader(USER_ID_HEADER) long userId) {
        log.info("Запрос GET /requests от пользователя {}", userId);
        return itemRequestClient.getOwnRequests(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getAllRequests(
            @RequestHeader(USER_ID_HEADER) long userId,
            @PositiveOrZero(message = "Параметр from не может быть отрицательным")
            @RequestParam(defaultValue = "0") int from,
            @Positive(message = "Параметр size должен быть больше 0")
            @RequestParam(defaultValue = "10") int size) {
        log.info("Запрос GET /requests/all?from={}&size={} от пользователя {}", from, size, userId);
        return itemRequestClient.getAllRequests(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getRequestById(@RequestHeader(USER_ID_HEADER) long userId,
                                                 @PathVariable Long requestId) {
        log.info("Запрос GET /requests/{} от пользователя {}", requestId, userId);
        return itemRequestClient.getRequest(userId, requestId);
    }
}
