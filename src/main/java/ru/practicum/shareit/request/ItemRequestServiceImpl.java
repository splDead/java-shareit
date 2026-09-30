package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto createRequest(Long userId, ItemRequestDto dto) {
        log.info("Сервис: Создание запроса пользователем id={}", userId);

        User requestor = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        ItemRequest request = ItemRequestMapper.toItemRequest(dto);
        request.setRequestor(requestor);
        request.setCreated(LocalDateTime.now());

        return ItemRequestMapper.toItemRequestDto(itemRequestRepository.save(request));
    }

    @Override
    public List<ItemRequestDto> getOwnRequests(Long userId) {
        log.info("Сервис: Получение собственных запросов пользователя id={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);

        return enrichRequestsWithItems(requests);
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long userId, int from, int size) {
        log.info("Сервис: Получение чужих запросов для пользователя id={} (from={}, size={})", userId, from, size);

        if (from < 0 || size <= 0) {
            throw new BadRequestException("Параметры пагинации 'from' и 'size' указаны неверно");
        }

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdNotOrderByCreatedDesc(userId, pageable);

        return enrichRequestsWithItems(requests);
    }

    @Override
    public ItemRequestDto getRequestById(Long userId, Long requestId) {
        log.info("Сервис: Получение запроса id={} пользователем id={}", requestId, userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        ItemRequest request = itemRequestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        ItemRequestDto dto = ItemRequestMapper.toItemRequestDto(request);

        List<ItemDto> items = itemRepository.findAllByRequestId(requestId).stream()
            .map(ItemMapper::toItemDto)
            .collect(Collectors.toList());
        dto.setItems(items);

        return dto;
    }

    private List<ItemRequestDto> enrichRequestsWithItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> requestIds = requests.stream().map(ItemRequest::getId).collect(Collectors.toList());

        Map<Long, List<ItemDto>> itemsByRequestId = itemRepository.findAllByRequestIdIn(requestIds).stream()
            .map(ItemMapper::toItemDto)
            .collect(Collectors.groupingBy(ItemDto::getRequestId));

        return requests.stream()
            .map(request -> {
                ItemRequestDto dto = ItemRequestMapper.toItemRequestDto(request);
                dto.setItems(itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList()));
                return dto;
            })
            .collect(Collectors.toList());
    }
}
