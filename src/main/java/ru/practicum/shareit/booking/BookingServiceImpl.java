package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingResponseDto create(Long userId, BookingRequestDto dto) {
        log.info("Создание бронирования пользователем id={} для вещи id={}", userId, dto.getItemId());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Item item = itemRepository.findById(dto.getItemId())
            .orElseThrow(() -> new NotFoundException("Вещь с id=" + dto.getItemId() + " не найдена"));

        if (!item.getAvailable()) {
            throw new BadRequestException("Вещь с id=" + item.getId() + " сейчас недоступна для бронирования");
        }

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец вещи не может забронировать сам у себя");
        }

        if (dto.getEnd().isBefore(dto.getStart()) || dto.getEnd().isEqual(dto.getStart())) {
            throw new BadRequestException("Дата окончания бронирования не может быть раньше или равна дате начала");
        }

        if (bookingRepository.existsByItemIdAndStatusAndStartLessThanAndEndGreaterThan(
                item.getId(), BookingStatus.APPROVED, dto.getEnd(), dto.getStart())) {
            throw new BadRequestException("Вещь с id=" + item.getId() + " уже забронирована на эти даты");
        }

        Booking booking = Booking.builder()
            .start(dto.getStart())
            .end(dto.getEnd())
            .item(item)
            .booker(user)
            .status(BookingStatus.WAITING)
            .build();

        return BookingMapper.toBookingResponseDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponseDto approve(Long userId, Long bookingId, Boolean approved) {
        log.info("Изменение статуса бронирования id={} пользователем id={}, статус={}", bookingId, userId, approved);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Только владелец вещи может подтверждать или отклонять бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BadRequestException("Статус бронирования уже изменен");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingResponseDto(bookingRepository.save(booking));
    }


    @Override
    public BookingResponseDto getById(Long userId, Long bookingId) {
        log.info("Получение бронирования id={} пользователем id={}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            throw new NotFoundException("Просмотр бронирования доступен только автору бронирования или владельцу вещи");
        }

        return BookingMapper.toBookingResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getAllByBooker(Long userId, String stateStr) {
        log.info("Получение списка бронирований арендатора id={} со статусом={}", userId, stateStr);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (parseState(stateStr)) {
            case ALL -> bookingRepository.findAllByBookerIdOrderByStartDesc(userId);
            case CURRENT -> bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case PAST -> bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
        };

        return bookings.stream().map(BookingMapper::toBookingResponseDto).collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDto> getAllByOwner(Long userId, String stateStr) {
        log.info("Получение списка бронирований вещей владельца id={} со статусом={}", userId, stateStr);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (parseState(stateStr)) {
            case ALL -> bookingRepository.findAllByItemOwnerIdOrderByStartDesc(userId);
            case CURRENT -> bookingRepository.findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case PAST -> bookingRepository.findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository.findAllByItemOwnerIdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
        };

        return bookings.stream().map(BookingMapper::toBookingResponseDto).collect(Collectors.toList());
    }

    private BookingState parseState(String stateStr) {
        try {
            return BookingState.valueOf(stateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown state: " + stateStr);
        }
    }
}
