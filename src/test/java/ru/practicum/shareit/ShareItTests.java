package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.BookingServiceImpl;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.CommentRepository;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.ItemServiceImpl;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserServiceImpl;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShareItTests {

	private UserRepository userRepository;
	private ItemRepository itemRepository;
	private BookingRepository bookingRepository;
	private CommentRepository commentRepository;

	private UserServiceImpl userService;
	private ItemServiceImpl itemService;
	private BookingServiceImpl bookingService;

	private User owner;
	private User booker;
	private Item item;

	@BeforeEach
	void setUp() {
		userRepository = Mockito.mock(UserRepository.class);
		itemRepository = Mockito.mock(ItemRepository.class);
		bookingRepository = Mockito.mock(BookingRepository.class);
		commentRepository = Mockito.mock(CommentRepository.class);

		userService = new UserServiceImpl(userRepository);
		itemService = new ItemServiceImpl(itemRepository, userRepository, bookingRepository, commentRepository);
		bookingService = new BookingServiceImpl(bookingRepository, userRepository, itemRepository);

		owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
		booker = User.builder().id(2L).name("Booker").email("booker@mail.ru").build();
		item = Item.builder().id(1L).name("Дрель").description("Ударная").available(true).owner(owner).build();
	}

	private BookingRequestDto bookingRequest(LocalDateTime start, LocalDateTime end) {
		return BookingRequestDto.builder().itemId(item.getId()).start(start).end(end).build();
	}

	private Booking booking(BookingStatus status, LocalDateTime start, LocalDateTime end) {
		return Booking.builder().id(1L).start(start).end(end).item(item).booker(booker).status(status).build();
	}

	@Test
	void shouldCreateUserAndFindHim() {
		UserDto userDto = UserDto.builder().name("Ivan").email("ivan@mail.ru").build();
		User savedUser = User.builder().id(1L).name("Ivan").email("ivan@mail.ru").build();

		when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
		when(userRepository.save(any(User.class))).thenReturn(savedUser);
		when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));

		UserDto created = userService.createUser(userDto);

		assertNotNull(created.getId());
		assertEquals("Ivan", created.getName());

		UserDto found = userService.getUserById(created.getId());
		assertEquals("ivan@mail.ru", found.getEmail());
	}

	@Test
	void shouldThrowExceptionWhenEmailIsDuplicate() {
		UserDto userDto = UserDto.builder().name("Petr").email("duplicate@mail.ru").build();
		when(userRepository.existsByEmailIgnoreCase("duplicate@mail.ru")).thenReturn(true);

		assertThrows(ConflictException.class, () -> userService.createUser(userDto));
	}

	@Test
	void shouldUpdateUserFieldsPartially() {
		User existingUser = User.builder().id(1L).name("Ivan").email("ivan@mail.ru").build();
		User savedUser = User.builder().id(1L).name("Ivan-Updated").email("ivan@mail.ru").build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserDto updateData = UserDto.builder().name("Ivan-Updated").build();
		UserDto updated = userService.updateUser(1L, updateData);

		assertEquals("Ivan-Updated", updated.getName());
		assertEquals("ivan@mail.ru", updated.getEmail());
	}

	@Test
	void shouldThrowExceptionWhenUpdateUserWithExistingEmail() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(userRepository.existsByEmailIgnoreCase("taken@mail.ru")).thenReturn(true);

		UserDto updateData = UserDto.builder().email("taken@mail.ru").build();
		assertThrows(ConflictException.class, () -> userService.updateUser(1L, updateData));
	}

	@Test
	void shouldDeleteUserCorrectly() {
		when(userRepository.existsById(1L)).thenReturn(true);
		assertDoesNotThrow(() -> userService.deleteUser(1L));
	}

	@Test
	void shouldThrowExceptionWhenDeleteNonExistentUser() {
		when(userRepository.existsById(999L)).thenReturn(false);
		assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
	}

	@Test
	void shouldReturnAllUsers() {
		when(userRepository.findAll()).thenReturn(List.of(owner, booker));
		assertEquals(2, userService.getAllUsers().size());
	}

	@Test
	void shouldAddItemWhenOwnerExists() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(itemRepository.save(any(Item.class))).thenReturn(item);

		ItemDto itemDto = ItemDto.builder().name("Дрель").description("Ударная").available(true).build();
		ItemDto createdItem = itemService.addItem(1L, itemDto);

		assertNotNull(createdItem.getId());
		assertEquals("Дрель", createdItem.getName());
	}

	@Test
	void shouldThrowExceptionWhenAddItemByNonExistentUser() {
		when(userRepository.findById(999L)).thenReturn(Optional.empty());
		ItemDto itemDto = ItemDto.builder().name("Дрель").description("Ударная").available(true).build();

		assertThrows(NotFoundException.class, () -> itemService.addItem(999L, itemDto));
	}

	@Test
	void shouldThrowExceptionWhenUpdateItemByNonOwner() {
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
		ItemDto updateData = ItemDto.builder().name("Супер Дрель").build();

		assertThrows(NotFoundException.class, () -> itemService.updateItem(2L, 1L, updateData));
	}

	@Test
	void shouldUpdateItemFieldsPartially() {
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

		ItemDto updated = itemService.updateItem(1L, 1L,
		ItemDto.builder().name("").description("Новое описание").available(false).build());

		assertEquals("Дрель", updated.getName());
		assertEquals("Новое описание", updated.getDescription());
		assertEquals(false, updated.getAvailable());
	}

	@Test
	void shouldSearchItemsByTextOnlyAvailable() {
		Item availableItem = Item.builder().id(1L).name("Шуруповерт Bosch").description("Крутой").available(true).build();
		when(itemRepository.searchAvailableItems("шуруп")).thenReturn(List.of(availableItem));

		List<ItemDto> searchResult = itemService.searchItems("шуруп");

		assertEquals(1, searchResult.size());
		assertEquals("Шуруповерт Bosch", searchResult.get(0).getName());
	}

	@Test
	void shouldReturnEmptyListWhenSearchTextIsEmpty() {
		List<ItemDto> result = itemService.searchItems("");
		assertTrue(result.isEmpty());
	}

	@Test
	void shouldReturnItemWithCommentsAndBookingsForOwnerOnly() {
		LocalDateTime now = LocalDateTime.now();
		Comment comment = Comment.builder().id(1L).text("Отличная дрель").item(item).author(booker).created(now).build();

		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
		when(commentRepository.findAllByItemId(1L)).thenReturn(List.of(comment));
		when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(1L)).thenReturn(List.of(
			booking(BookingStatus.APPROVED, now.minusDays(2), now.minusDays(1)),
			Booking.builder().id(2L).start(now.plusDays(1)).end(now.plusDays(2))
				.item(item).booker(booker).status(BookingStatus.APPROVED).build()));

		ItemDto forOwner = itemService.getItemById(1L, 1L);
		assertEquals(1, forOwner.getComments().size());
		assertEquals("Booker", forOwner.getComments().get(0).getAuthorName());
		assertEquals(1L, forOwner.getLastBooking().getId());
		assertEquals(2L, forOwner.getNextBooking().getId());

		ItemDto forStranger = itemService.getItemById(1L, 99L);
		assertNull(forStranger.getLastBooking());
		assertNull(forStranger.getNextBooking());
	}

	@Test
	void shouldNotCountWaitingBookingAsLastOrNext() {
		LocalDateTime now = LocalDateTime.now();

		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
		when(commentRepository.findAllByItemId(1L)).thenReturn(List.of());
		when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(1L))
			.thenReturn(List.of(booking(BookingStatus.WAITING, now.minusDays(2), now.minusDays(1))));

		ItemDto dto = itemService.getItemById(1L, 1L);
		assertNull(dto.getLastBooking());
		assertNull(dto.getNextBooking());
	}

	@Test
	void shouldReturnOwnerItemsWithCommentsInOneQuery() {
		LocalDateTime now = LocalDateTime.now();
		Comment comment = Comment.builder().id(1L).text("Ок").item(item).author(booker).created(now).build();

		when(itemRepository.findAllByOwnerIdOrderByIdAsc(1L)).thenReturn(List.of(item));
		when(commentRepository.findAllByItemIdIn(List.of(1L))).thenReturn(List.of(comment));
		when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(1L)).thenReturn(List.of());

		List<ItemDto> items = itemService.getItemsByOwner(1L);

		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getComments().size());
		Mockito.verify(commentRepository, Mockito.times(1)).findAllByItemIdIn(any());
	}

	@Test
	void shouldReturnEmptyListWhenOwnerHasNoItems() {
		when(itemRepository.findAllByOwnerIdOrderByIdAsc(1L)).thenReturn(List.of());
		assertTrue(itemService.getItemsByOwner(1L).isEmpty());
	}

	@Test
	void shouldAddCommentAfterFinishedBooking() {
		LocalDateTime now = LocalDateTime.now();
		Comment saved = Comment.builder().id(1L).text("Всё ок").item(item).author(booker).created(now).build();

		when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
		when(bookingRepository.findAllByBookerIdAndItemIdAndStatusAndEndBefore(
			eq(2L), eq(1L), eq(BookingStatus.APPROVED), any(LocalDateTime.class)))
			.thenReturn(List.of(booking(BookingStatus.APPROVED, now.minusDays(2), now.minusDays(1))));
		when(commentRepository.save(any(Comment.class))).thenReturn(saved);

		CommentRequestDto request = new CommentRequestDto();
		request.setText("Всё ок");
		CommentResponseDto response = itemService.addComment(2L, 1L, request);

		assertEquals("Всё ок", response.getText());
		assertEquals("Booker", response.getAuthorName());
	}

	@Test
	void shouldThrowExceptionWhenCommentBookingIsNotFinished() {
		when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
		when(bookingRepository.findAllByBookerIdAndItemIdAndStatusAndEndBefore(
			anyLong(), anyLong(), any(BookingStatus.class), any(LocalDateTime.class)))
			.thenReturn(List.of());

		CommentRequestDto request = new CommentRequestDto();
		request.setText("Рано");

		assertThrows(BadRequestException.class, () -> itemService.addComment(2L, 1L, request));
	}

	@Test
	void shouldCreateBookingWithWaitingStatus() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		LocalDateTime end = start.plusDays(1);

		when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.of(item));
		when(bookingRepository.save(any(Booking.class))).thenReturn(booking(BookingStatus.WAITING, start, end));

		BookingResponseDto created = bookingService.create(2L, bookingRequest(start, end));

		assertEquals(BookingStatus.WAITING, created.getStatus());
		assertEquals(2L, created.getBooker().getId());
		assertEquals("Дрель", created.getItem().getName());
	}

	@Test
	void shouldThrowExceptionWhenBookingUnavailableItem() {
		item.setAvailable(false);
		when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.of(item));

		LocalDateTime start = LocalDateTime.now().plusDays(1);
		assertThrows(BadRequestException.class, () -> bookingService.create(2L, bookingRequest(start, start.plusDays(1))));
	}

	@Test
	void shouldThrowForbiddenWhenOwnerBooksOwnItem() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.of(item));

		LocalDateTime start = LocalDateTime.now().plusDays(1);
		assertThrows(ForbiddenException.class, () -> bookingService.create(1L, bookingRequest(start, start.plusDays(1))));
	}

	@Test
	void shouldThrowExceptionWhenBookingEndEqualsStartBeforeTouchingDatabase() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);

		assertThrows(BadRequestException.class, () -> bookingService.create(2L, bookingRequest(start, start)));
		Mockito.verifyNoInteractions(userRepository, itemRepository);
	}

	@Test
	void shouldThrowExceptionWhenBookingDatesOverlapApprovedBooking() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		LocalDateTime end = start.plusDays(1);

		when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.of(item));
		when(bookingRepository.existsByItemIdAndStatusAndStartLessThanAndEndGreaterThan(
				1L, BookingStatus.APPROVED, end, start)).thenReturn(true);

		assertThrows(BadRequestException.class, () -> bookingService.create(2L, bookingRequest(start, end)));
	}

	@Test
	void shouldThrowExceptionWhenBookingItemNotFound() {
		when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

		LocalDateTime start = LocalDateTime.now().plusDays(1);
		assertThrows(NotFoundException.class, () -> bookingService.create(2L, bookingRequest(start, start.plusDays(1))));
	}

	@Test
	void shouldApproveAndRejectBookingByOwner() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);

		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.of(item));
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.WAITING, start, start.plusDays(1))));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		assertEquals(BookingStatus.APPROVED, bookingService.approve(1L, 1L, true).getStatus());

		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.WAITING, start, start.plusDays(1))));
		assertEquals(BookingStatus.REJECTED, bookingService.approve(1L, 1L, false).getStatus());
	}

	@Test
	void shouldThrowExceptionWhenApproveOverlapsAnotherApprovedBooking() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		LocalDateTime end = start.plusDays(1);

		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.WAITING, start, end)));
		when(itemRepository.findByIdWithLock(1L)).thenReturn(Optional.of(item));
		when(bookingRepository.existsByItemIdAndStatusAndStartLessThanAndEndGreaterThan(
			1L, BookingStatus.APPROVED, end, start)).thenReturn(true);

		assertThrows(BadRequestException.class, () -> bookingService.approve(1L, 1L, true));
	}

	@Test
	void shouldRejectBookingEvenWhenDatesAreTaken() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);

		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.WAITING, start, start.plusDays(1))));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		assertEquals(BookingStatus.REJECTED, bookingService.approve(1L, 1L, false).getStatus());
		Mockito.verify(itemRepository, Mockito.never()).findByIdWithLock(anyLong());
	}

	@Test
	void shouldThrowForbiddenWhenApproveByNonOwner() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.WAITING, start, start.plusDays(1))));

		assertThrows(ForbiddenException.class, () -> bookingService.approve(2L, 1L, true));
	}

	@Test
	void shouldThrowExceptionWhenApproveAlreadyProcessedBooking() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.APPROVED, start, start.plusDays(1))));

		assertThrows(BadRequestException.class, () -> bookingService.approve(1L, 1L, true));
	}

	@Test
	void shouldGetBookingByBookerAndOwnerButNotByStranger() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(BookingStatus.WAITING, start, start.plusDays(1))));

		assertEquals(1L, bookingService.getById(2L, 1L).getId());
		assertEquals(1L, bookingService.getById(1L, 1L).getId());
		assertThrows(NotFoundException.class, () -> bookingService.getById(99L, 1L));
	}

	@Test
	void shouldThrowExceptionWhenBookingNotFound() {
		when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> bookingService.getById(1L, 99L));
	}

	@Test
	void shouldReturnBookerBookingsForEveryState() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		Booking existing = booking(BookingStatus.WAITING, start, start.plusDays(1));

		when(userRepository.existsById(2L)).thenReturn(true);
		when(bookingRepository.findAllByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(existing));
		when(bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(eq(2L), any(), any()))
			.thenReturn(List.of(existing));
		when(bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(eq(2L), any())).thenReturn(List.of(existing));
		when(bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(eq(2L), any())).thenReturn(List.of(existing));
		when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(eq(2L), any())).thenReturn(List.of(existing));

		for (BookingState state : BookingState.values()) {
			assertEquals(1, bookingService.getAllByBooker(2L, state).size(), "state=" + state);
		}
	}

	@Test
	void shouldReturnOwnerBookingsForEveryState() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		Booking existing = booking(BookingStatus.WAITING, start, start.plusDays(1));

		when(userRepository.existsById(1L)).thenReturn(true);
		when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(1L)).thenReturn(List.of(existing));
		when(bookingRepository.findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(eq(1L), any(), any()))
			.thenReturn(List.of(existing));
		when(bookingRepository.findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(eq(1L), any())).thenReturn(List.of(existing));
		when(bookingRepository.findAllByItemOwnerIdAndStartAfterOrderByStartDesc(eq(1L), any())).thenReturn(List.of(existing));
		when(bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(eq(1L), any())).thenReturn(List.of(existing));

		for (BookingState state : BookingState.values()) {
			assertEquals(1, bookingService.getAllByOwner(1L, state).size(), "state=" + state);
		}
	}

	@Test
	void shouldReturnBadRequestWhenStateIsUnknown() throws Exception {
		BookingService bookingServiceMock = Mockito.mock(BookingService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingServiceMock))
			.setControllerAdvice(new ErrorHandler())
			.build();

		mockMvc.perform(get("/bookings").header("X-Sharer-User-Id", 2L).param("state", "UNSUPPORTED"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Unknown state: UNSUPPORTED"));

		mockMvc.perform(get("/bookings").header("X-Sharer-User-Id", 2L))
			.andExpect(status().isOk());
		Mockito.verify(bookingServiceMock).getAllByBooker(2L, BookingState.ALL);
	}

	@Test
	void shouldThrowExceptionWhenBookingsRequestedByUnknownUser() {
		when(userRepository.existsById(99L)).thenReturn(false);
		assertThrows(NotFoundException.class, () -> bookingService.getAllByBooker(99L, BookingState.ALL));
		assertThrows(NotFoundException.class, () -> bookingService.getAllByOwner(99L, BookingState.ALL));
	}

	@Test
	void shouldMapItemToDtoAndBack() {
		Item mapped = Item.builder().id(10L).name("Молоток").description("Тяжелый").available(true).requestId(2L).build();

		ItemDto dto = ItemMapper.toItemDto(mapped);
		assertNotNull(dto);
		assertEquals(10L, dto.getId());
		assertEquals(2L, dto.getRequestId());

		Item mappedBack = ItemMapper.toItem(dto);
		assertEquals("Молоток", mappedBack.getName());
		assertEquals(2L, mappedBack.getRequestId());
	}

	@Test
	void shouldMapUserToDtoAndBack() {
		UserDto dto = UserMapper.toUserDto(booker);
		assertEquals("Booker", dto.getName());

		User mappedBack = UserMapper.toUser(dto);
		assertEquals("booker@mail.ru", mappedBack.getEmail());
	}

	@Test
	void shouldMapNullsToNull() {
		assertNull(ItemMapper.toItemDto(null));
		assertNull(ItemMapper.toItem(null));
		assertNull(ItemMapper.toCommentResponseDto(null));
		assertNull(UserMapper.toUserDto(null));
		assertNull(UserMapper.toUser(null));
	}
}
