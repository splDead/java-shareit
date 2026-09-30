package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto addItem(Long ownerId, ItemDto itemDto) {
        log.info("Добавление вещи пользователем с id={}", ownerId);

        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new NotFoundException("Пользователь с id=" + ownerId + " не найден"));

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long ownerId, Long itemId, ItemDto itemDto) {
        log.info("Обновление вещи с id={} пользователем с id={}", itemId, ownerId);

        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        if (!item.getOwner().getId().equals(ownerId)) {
            throw new NotFoundException("Пользователь с id=" + ownerId + " не владелец вещи");
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

        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto getItemById(Long itemId, Long userId) {
        log.info("Получение вещи с id={} пользователем id={}", itemId, userId);

        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        ItemDto dto = ItemMapper.toItemDto(item);
        dto.setComments(toCommentDtos(commentRepository.findAllByItemId(itemId)));

        if (userId != null && item.getOwner().getId().equals(userId)) {
            enrichWithBookings(dto, bookingRepository.findAllByItemOwnerIdOrderByStartDesc(userId));
        }

        return dto;
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long ownerId) {
        log.info("Получение списка вещей владельца с id={}", ownerId);

        List<Item> items = itemRepository.findAllByOwnerIdOrderByIdAsc(ownerId);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());
        Map<Long, List<CommentResponseDto>> commentsByItemId = commentRepository.findAllByItemIdIn(itemIds).stream()
            .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                Collectors.mapping(ItemMapper::toCommentResponseDto, Collectors.toList())));

        List<Booking> ownerBookings = bookingRepository.findAllByItemOwnerIdOrderByStartDesc(ownerId);

        return items.stream()
            .map(item -> {
                ItemDto dto = ItemMapper.toItemDto(item);
                dto.setComments(commentsByItemId.getOrDefault(item.getId(), Collections.emptyList()));
                enrichWithBookings(dto, ownerBookings);
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        log.info("Поиск вещей по запросу: '{}'", text);

        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        return itemRepository.searchAvailableItems(text).stream()
            .map(ItemMapper::toItemDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponseDto addComment(Long userId, Long itemId, CommentRequestDto commentDto) {
        log.info("Добавление отзыва пользователем id={} к вещи id={}", userId, itemId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = bookingRepository
            .findAllByBookerIdAndItemIdAndStatusAndEndBefore(userId, itemId, BookingStatus.APPROVED, now);

        if (bookings.isEmpty()) {
            throw new BadRequestException("Оставить отзыв можно только после завершения одобренной аренды вещи");
        }

        Comment comment = Comment.builder()
            .text(commentDto.getText())
            .item(item)
            .author(user)
            .created(now)
            .build();

        return ItemMapper.toCommentResponseDto(commentRepository.save(comment));
    }

    private List<CommentResponseDto> toCommentDtos(List<Comment> comments) {
        return comments.stream()
            .map(ItemMapper::toCommentResponseDto)
            .collect(Collectors.toList());
    }

    private void enrichWithBookings(ItemDto dto, List<Booking> ownerBookings) {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> itemBookings = ownerBookings.stream()
            .filter(b -> b.getItem().getId().equals(dto.getId()))
            .filter(b -> b.getStatus() == BookingStatus.APPROVED)
            .collect(Collectors.toList());

        itemBookings.stream()
            .filter(b -> !b.getStart().isAfter(now))
            .max(Comparator.comparing(Booking::getStart))
            .ifPresent(b -> dto.setLastBooking(new ItemDto.BookingInfo(b.getId(), b.getBooker().getId())));

        itemBookings.stream()
            .filter(b -> b.getStart().isAfter(now))
            .min(Comparator.comparing(Booking::getStart))
            .ifPresent(b -> dto.setNextBooking(new ItemDto.BookingInfo(b.getId(), b.getBooker().getId())));
    }
}
