package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerControllersTests {

	private static final String USER_ID_HEADER = "X-Sharer-User-Id";

	private UserService userService;
	private ItemService itemService;
	private BookingService bookingService;
	private ItemRequestService itemRequestService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userService = Mockito.mock(UserService.class);
		itemService = Mockito.mock(ItemService.class);
		bookingService = Mockito.mock(BookingService.class);
		itemRequestService = Mockito.mock(ItemRequestService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService), new ItemController(itemService),
				new BookingController(bookingService), new ItemRequestController(itemRequestService))
			.setControllerAdvice(new ErrorHandler())
			.build();
	}

	private static UserDto user() {
		return UserDto.builder().id(1L).name("Ivan").email("ivan@mail.ru").build();
	}

	private static ItemDto item() {
		return ItemDto.builder().id(1L).name("Дрель").description("Ударная").available(true).requestId(5L).build();
	}

	private static BookingResponseDto booking() {
		return BookingResponseDto.builder().id(1L).status(BookingStatus.WAITING)
			.booker(new BookingResponseDto.BookerDto(2L))
			.item(new BookingResponseDto.ItemDto(1L, "Дрель"))
			.build();
	}

	private static ItemRequestDto request() {
		return ItemRequestDto.builder().id(5L).description("Нужна дрель").requestorId(2L)
			.items(List.of(new ItemRequestDto.ItemAnswerDto(1L, "Дрель", 1L)))
			.build();
	}

	@Test
	void shouldHandleUserEndpoints() throws Exception {
		when(userService.createUser(any())).thenReturn(user());
		when(userService.updateUser(eq(1L), any())).thenReturn(user());
		when(userService.getUserById(1L)).thenReturn(user());
		when(userService.getAllUsers()).thenReturn(List.of(user()));

		mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ivan\",\"email\":\"ivan@mail.ru\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.email").value("ivan@mail.ru"));
		mockMvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ivan\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Ivan"));
		mockMvc.perform(get("/users/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1));
		mockMvc.perform(get("/users"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(delete("/users/1"))
			.andExpect(status().isOk());

		verify(userService).deleteUser(1L);
	}

	@Test
	void shouldHandleItemEndpoints() throws Exception {
		when(itemService.addItem(eq(1L), any())).thenReturn(item());
		when(itemService.updateItem(eq(1L), eq(1L), any())).thenReturn(item());
		when(itemService.getItemById(1L, 1L)).thenReturn(item());
		when(itemService.getItemsByOwner(1L)).thenReturn(List.of(item()));
		when(itemService.searchItems("дрель")).thenReturn(List.of(item()));
		when(itemService.addComment(eq(2L), eq(1L), any()))
			.thenReturn(CommentResponseDto.builder().id(1L).text("Ок").authorName("Booker").build());

		mockMvc.perform(post("/items").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true,\"requestId\":5}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requestId").value(5));
		mockMvc.perform(patch("/items/1").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"available\":false}"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/items/1").header(USER_ID_HEADER, 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Дрель"));
		mockMvc.perform(get("/items").header(USER_ID_HEADER, 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/items/search").param("text", "дрель"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1));
		mockMvc.perform(post("/items/1/comment").header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"Ок\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authorName").value("Booker"));
	}

	@Test
	void shouldHandleBookingEndpoints() throws Exception {
		when(bookingService.create(eq(2L), any())).thenReturn(booking());
		when(bookingService.approve(1L, 1L, true)).thenReturn(booking());
		when(bookingService.getById(2L, 1L)).thenReturn(booking());
		when(bookingService.getAllByOwner(1L, BookingState.FUTURE)).thenReturn(List.of(booking()));

		mockMvc.perform(post("/bookings").header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemId\":1,\"start\":\"2030-01-01T10:00:00\",\"end\":\"2030-01-02T10:00:00\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("WAITING"))
			.andExpect(jsonPath("$.item.name").value("Дрель"));
		mockMvc.perform(patch("/bookings/1").header(USER_ID_HEADER, 1).param("approved", "true"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/bookings/1").header(USER_ID_HEADER, 2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.booker.id").value(2));
		mockMvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 1).param("state", "FUTURE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void shouldHandleRequestEndpoints() throws Exception {
		when(itemRequestService.createRequest(eq(2L), any())).thenReturn(request());
		when(itemRequestService.getOwnRequests(2L)).thenReturn(List.of(request()));
		when(itemRequestService.getAllRequests(1L, 0, 10)).thenReturn(List.of(request()));
		when(itemRequestService.getRequestById(1L, 5L)).thenReturn(request());

		mockMvc.perform(post("/requests").header(USER_ID_HEADER, 2).contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"Нужна дрель\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.description").value("Нужна дрель"));
		mockMvc.perform(get("/requests").header(USER_ID_HEADER, 2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].items[0].ownerId").value(1));
		mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/requests/5").header(USER_ID_HEADER, 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].name").value("Дрель"));
	}

	@Test
	void shouldMapExceptionsToHttpStatuses() throws Exception {
		when(userService.getUserById(1L)).thenThrow(new NotFoundException("не найден"));
		when(userService.getUserById(2L)).thenThrow(new BadRequestException("плохой запрос"));
		when(userService.getUserById(3L)).thenThrow(new ForbiddenException("запрещено"));
		when(userService.getUserById(4L)).thenThrow(new ConflictException("конфликт"));
		when(userService.getUserById(5L)).thenThrow(new IllegalStateException("boom"));

		mockMvc.perform(get("/users/1")).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("не найден"));
		mockMvc.perform(get("/users/2")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("плохой запрос"));
		mockMvc.perform(get("/users/3")).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("запрещено"));
		mockMvc.perform(get("/users/4")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("конфликт"));
		mockMvc.perform(get("/users/5")).andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.error").exists());
		mockMvc.perform(get("/users/abc")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Некорректное значение параметра 'userId': abc"));
		mockMvc.perform(get("/items")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Required request header 'X-Sharer-User-Id' is not present"));
	}
}
