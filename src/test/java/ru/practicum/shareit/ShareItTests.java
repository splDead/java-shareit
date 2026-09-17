package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.ItemServiceImpl;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserServiceImpl;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ShareItTests {

	private UserRepository userRepository;
	private ItemRepository itemRepository;
	private UserServiceImpl userService;
	private ItemServiceImpl itemService;

	@BeforeEach
	void setUp() {
		userRepository = new UserRepository();
		itemRepository = new ItemRepository();

		userService = new UserServiceImpl(userRepository);
		itemService = new ItemServiceImpl(itemRepository, userRepository);
	}

	@Test
	void shouldCreateUserAndFindHim() {
		UserDto userDto = UserDto.builder().name("Ivan").email("ivan@mail.ru").build();

		UserDto created = userService.createUser(userDto);

		assertNotNull(created.getId());
		assertEquals("Ivan", created.getName());

		UserDto found = userService.getUserById(created.getId());
		assertEquals("ivan@mail.ru", found.getEmail());
	}

	@Test
	void shouldThrowExceptionWhenEmailIsDuplicate() {
		UserDto user1 = UserDto.builder().name("Ivan").email("duplicate@mail.ru").build();
		UserDto user2 = UserDto.builder().name("Petr").email("duplicate@mail.ru").build();

		userService.createUser(user1);

		assertThrows(ConflictException.class, () -> userService.createUser(user2));
	}

	@Test
	void shouldUpdateUserFieldsPartially() {
		UserDto userDto = UserDto.builder().name("Ivan").email("ivan@mail.ru").build();
		UserDto created = userService.createUser(userDto);

		UserDto updateData = UserDto.builder().name("Ivan-Updated").build();
		UserDto updated = userService.updateUser(created.getId(), updateData);

		assertEquals("Ivan-Updated", updated.getName());
		assertEquals("ivan@mail.ru", updated.getEmail()); // Email не должен затереться
	}

	@Test
	void shouldDeleteUserCorrectly() {
		UserDto created = userService.createUser(UserDto.builder().name("User").email("u@mail.ru").build());

		userService.deleteUser(created.getId());

		assertThrows(NoSuchElementException.class, () -> userService.getUserById(created.getId()));
	}

	@Test
	void shouldAddItemWhenOwnerExists() {
		UserDto owner = userService.createUser(UserDto.builder().name("Owner").email("owner@mail.ru").build());
		ItemDto itemDto = ItemDto.builder().name("Дрель").description("Ударная").available(true).build();

		ItemDto createdItem = itemService.addItem(owner.getId(), itemDto);

		assertNotNull(createdItem.getId());
		assertEquals("Дрель", createdItem.getName());
	}

	@Test
	void shouldThrowExceptionWhenAddItemByNonExistentUser() {
		ItemDto itemDto = ItemDto.builder().name("Дрель").description("Ударная").available(true).build();

		assertThrows(NoSuchElementException.class, () -> itemService.addItem(999L, itemDto));
	}

	@Test
	void shouldThrowExceptionWhenUpdateItemByNonOwner() {
		UserDto owner = userService.createUser(UserDto.builder().name("Owner").email("owner@mail.ru").build());
		UserDto stranger = userService.createUser(UserDto.builder().name("Stranger").email("stranger@mail.ru").build());

		ItemDto itemDto = ItemDto.builder().name("Дрель").description("Ударная").available(true).build();
		ItemDto createdItem = itemService.addItem(owner.getId(), itemDto);

		ItemDto updateData = ItemDto.builder().name("Супер Дрель").build();

		assertThrows(NoSuchElementException.class, () ->
			itemService.updateItem(stranger.getId(), createdItem.getId(), updateData));
	}

	@Test
	void shouldSearchItemsByTextOnlyAvailable() {
		UserDto owner = userService.createUser(UserDto.builder().name("Owner").email("o@mail.ru").build());

		itemService.addItem(owner.getId(), ItemDto.builder().name("Шуруповерт Bosch").description("Крутой").available(true).build());
		itemService.addItem(owner.getId(), ItemDto.builder().name("Старый шуруповерт").description("Сломан").available(false).build());

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
	void shouldMapItemToDtoAndBack() {
		Item item = Item.builder().id(10L).name("Молоток").description("Тяжелый").available(true).requestId(2L).build();

		ItemDto dto = ItemMapper.toItemDto(item);
		assertNotNull(dto);
		assertEquals(10L, dto.getId());
		assertEquals(2L, dto.getRequestId());

		Item mappedBack = ItemMapper.toItem(dto);
		assertEquals("Молоток", mappedBack.getName());
		assertEquals(2L, mappedBack.getRequestId());
	}

	@Test
	void shouldMapUserToDtoAndBack() {
		User user = User.builder().id(1L).name("Max").email("max@mail.ru").build();

		UserDto dto = UserMapper.toUserDto(user);
		assertEquals("Max", dto.getName());

		User mappedBack = UserMapper.toUser(dto);
		assertEquals("max@mail.ru", mappedBack.getEmail());
	}
}
