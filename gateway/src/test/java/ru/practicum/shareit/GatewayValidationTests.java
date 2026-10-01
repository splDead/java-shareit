package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.UserController;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayValidationTests {

	private static final String USER_ID_HEADER = "X-Sharer-User-Id";

	private UserClient userClient;
	private ItemClient itemClient;
	private BookingClient bookingClient;
	private ItemRequestClient itemRequestClient;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userClient = Mockito.mock(UserClient.class);
		itemClient = Mockito.mock(ItemClient.class);
		bookingClient = Mockito.mock(BookingClient.class);
		itemRequestClient = Mockito.mock(ItemRequestClient.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userClient), new ItemController(itemClient),
				new BookingController(bookingClient), new ItemRequestController(itemRequestClient))
			.setControllerAdvice(new ErrorHandler())
			.build();
	}

	@Test
	void shouldRejectInvalidRequestsWithoutCallingServer() throws Exception {
		mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ivan\",\"email\":\"not-an-email\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Некорректный формат email"));

		mockMvc.perform(post("/items").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Дрель\",\"description\":\"Ударная\"}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(post("/requests").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\" \"}"))
			.andExpect(status().isBadRequest());

		LocalDateTime start = LocalDateTime.now().plusDays(2);
		mockMvc.perform(post("/bookings").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemId\":1,\"start\":\"" + start + "\",\"end\":\"" + start.minusDays(1) + "\"}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 1).param("size", "0"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/bookings").header(USER_ID_HEADER, 1).param("state", "UNSUPPORTED"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Unknown state: UNSUPPORTED"));

		mockMvc.perform(get("/requests"))
			.andExpect(status().isBadRequest());

		Mockito.verifyNoInteractions(userClient, itemClient, bookingClient, itemRequestClient);
	}

	@Test
	void shouldForwardValidRequestsAndPassServerStatusThrough() throws Exception {
		when(itemClient.addItem(eq(1L), any())).thenReturn(ResponseEntity.status(404).build());
		mockMvc.perform(post("/items").header(USER_ID_HEADER, 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true,\"requestId\":7}"))
			.andExpect(status().isNotFound());

		when(itemRequestClient.getAllRequests(1L, 0, 10)).thenReturn(ResponseEntity.ok().build());
		mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 1))
			.andExpect(status().isOk());

		when(bookingClient.getOwnerBookings(1L, BookingState.WAITING)).thenReturn(ResponseEntity.ok().build());
		mockMvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 1).param("state", "waiting"))
			.andExpect(status().isOk());
	}
}
