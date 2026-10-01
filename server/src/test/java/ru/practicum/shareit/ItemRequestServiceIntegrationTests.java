package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ItemRequestServiceIntegrationTests {

	@Autowired
	private UserService userService;
	@Autowired
	private ItemService itemService;
	@Autowired
	private ItemRequestService itemRequestService;

	private Long createUser(String name) {
		return userService.createUser(UserDto.builder().name(name).email(name + "@mail.ru").build()).getId();
	}

	private ItemRequestDto createRequest(Long userId, String description) {
		return itemRequestService.createRequest(userId, ItemRequestDto.builder().description(description).build());
	}

	@Test
	void shouldReturnRequestsWithAnswersNewestFirst() {
		Long requestor = createUser("requestor");
		Long owner = createUser("owner");
		ItemRequestDto drill = createRequest(requestor, "Нужна дрель");
		ItemRequestDto saw = createRequest(requestor, "Нужна пила");
		assertNotNull(drill.getCreated());

		ItemDto answer = itemService.addItem(owner, ItemDto.builder()
			.name("Дрель").description("Ударная").available(true).requestId(drill.getId()).build());
		assertEquals(drill.getId(), answer.getRequestId());

		List<ItemRequestDto> own = itemRequestService.getOwnRequests(requestor);
		assertEquals(List.of(saw.getId(), drill.getId()), own.stream().map(ItemRequestDto::getId).toList());
		assertTrue(own.get(0).getItems().isEmpty());
		assertEquals(List.of(new ItemRequestDto.ItemAnswerDto(answer.getId(), "Дрель", owner)), own.get(1).getItems());

		ItemRequestDto byId = itemRequestService.getRequestById(owner, drill.getId());
		assertEquals("Нужна дрель", byId.getDescription());
		assertEquals(requestor, byId.getRequestorId());
		assertEquals(1, byId.getItems().size());
	}

	@Test
	void shouldReturnOnlyOtherUsersRequestsWithPagination() {
		Long requestor = createUser("author");
		Long viewer = createUser("viewer");
		createRequest(requestor, "Первый");
		createRequest(requestor, "Второй");
		createRequest(viewer, "Свой");

		List<ItemRequestDto> firstPage = itemRequestService.getAllRequests(viewer, 0, 1);
		assertEquals(1, firstPage.size());
		assertEquals("Второй", firstPage.get(0).getDescription());

		assertEquals(2, itemRequestService.getAllRequests(viewer, 0, 10).size());
		assertTrue(itemRequestService.getAllRequests(viewer, 10, 10).isEmpty());
		assertTrue(itemRequestService.getOwnRequests(createUser("newbie")).isEmpty());
	}

	@Test
	void shouldThrowForUnknownEntitiesAndBadPagination() {
		Long user = createUser("someone");

		assertThrows(NotFoundException.class, () -> createRequest(999L, "Нет автора"));
		assertThrows(NotFoundException.class, () -> itemRequestService.getOwnRequests(999L));
		assertThrows(NotFoundException.class, () -> itemRequestService.getAllRequests(999L, 0, 10));
		assertThrows(NotFoundException.class, () -> itemRequestService.getRequestById(999L, 1L));
		assertThrows(NotFoundException.class, () -> itemRequestService.getRequestById(user, 999L));
		assertThrows(BadRequestException.class, () -> itemRequestService.getAllRequests(user, -1, 10));
		assertThrows(BadRequestException.class, () -> itemRequestService.getAllRequests(user, 0, 0));
	}
}
