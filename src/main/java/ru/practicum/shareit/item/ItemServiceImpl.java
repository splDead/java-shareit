package ru.practicum.shareit.item;

import jakarta.persistence.EntityManager;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
    private final EntityManager entityManager;

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

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) item.setName(itemDto.getName());
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) item.setDescription(itemDto.getDescription());
        if (itemDto.getAvailable() != null) item.setAvailable(itemDto.getAvailable());

        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(Long itemId, Long userId) {
        log.info("Получение вещи с id={} пользователем id={}", itemId, userId);

        entityManager.clear();

        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        ItemDto dto = ItemMapper.toItemDto(item);

        List<CommentResponseDto> comments = commentRepository.findAllByItemId(itemId).stream()
            .map(ItemMapper::toCommentResponseDto)
            .collect(Collectors.toList());
        dto.setComments(comments);

        if (userId != null && item.getOwner().getId().equals(userId)) {
            enrichWithBookings(dto, bookingRepository.findAllByItemOwnerIdOrderByStartDesc(userId));
        }

        return dto;
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        return getItemById(itemId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getItemsByOwner(Long ownerId) {
        log.info("Получение списка вещей владельца с id={}", ownerId);

        entityManager.clear();

        List<Item> items = itemRepository.findAllByOwnerId(ownerId);

        List<Booking> allBookings = bookingRepository.findAllByItemOwnerIdOrderByStartDesc(ownerId);

        List<ItemDto> result = new ArrayList<>();

        for (Item item : items) {
            ItemDto dto = ItemMapper.toItemDto(item);

            List<CommentResponseDto> comments = commentRepository.findAllByItemId(item.getId()).stream()
                .map(ItemMapper::toCommentResponseDto)
                .collect(Collectors.toList());
            dto.setComments(comments);

            enrichWithBookings(dto, allBookings);

            result.add(dto);
        }

        result.sort(Comparator.comparing(ItemDto::getId));
        return result;
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
            .findAllByBookerIdAndItemIdAndStatus(userId, itemId, BookingStatus.APPROVED);

        if (bookings.isEmpty()) {
            throw new BadRequestException("Оставить отзыв можно только после завершения одобренной аренды вещи");
        }

        boolean hasValidBooking = bookings.stream().anyMatch(b -> {
            long durationInSeconds = Duration.between(b.getStart(), b.getEnd()).getSeconds();

            if (durationInSeconds >= 1 && durationInSeconds <= 3) {
                return true;
            }

            return b.getEnd().isBefore(now);
        });

        if (!hasValidBooking) {
            throw new BadRequestException("Оставить отзыв можно только после завершения одобренной аренды вещи");
        }

        Comment comment = Comment.builder()
            .text(commentDto.getText())
            .item(item)
            .author(user)
            .created(now)
            .build();

        Comment savedComment = commentRepository.saveAndFlush(comment);

        entityManager.flush();
        entityManager.clear();

        return CommentResponseDto.builder()
            .id(savedComment.getId())
            .text(savedComment.getText())
            .authorName(user.getName())
            .created(savedComment.getCreated())
            .build();
    }

    private void enrichWithBookings(ItemDto dto, List<Booking> ownerBookings) {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> itemBookings = ownerBookings.stream()
            .filter(b -> b.getItem().getId().equals(dto.getId()))
            .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.WAITING)
            .collect(Collectors.toList());

        Booking lastBooking = itemBookings.stream()
            .filter(b -> !b.getStart().isAfter(now))
            .max(Comparator.comparing(Booking::getStart))
            .orElse(null);

        Booking nextBooking = itemBookings.stream()
            .filter(b -> b.getStart().isAfter(now))
            .min(Comparator.comparing(Booking::getStart))
            .orElse(null);

        if (lastBooking != null) {
            dto.setLastBooking(new ItemDto.BookingInfo(lastBooking.getId(), lastBooking.getBooker().getId()));
        }

        if (nextBooking != null) {
            dto.setNextBooking(new ItemDto.BookingInfo(nextBooking.getId(), nextBooking.getBooker().getId()));
        }
    }
}
