package ru.practicum.shareit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException; // Заменили тип исключения на 404
import ru.practicum.shareit.item.CommentRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ShareItTests {

	private UserRepository userRepository;
	private ItemRepository itemRepository;
	private BookingRepository bookingRepository;
	private CommentRepository commentRepository;
	private EntityManager entityManager;

	private UserServiceImpl userService;
	private ItemServiceImpl itemService;

	@BeforeEach
	void setUp() {
		userRepository = Mockito.mock(UserRepository.class);
		itemRepository = Mockito.mock(ItemRepository.class);
		bookingRepository = Mockito.mock(BookingRepository.class);
		commentRepository = Mockito.mock(CommentRepository.class);
		entityManager = Mockito.mock(EntityManager.class);

		userService = new UserServiceImpl(userRepository);

		itemService = new ItemServiceImpl(itemRepository, userRepository, bookingRepository, commentRepository, entityManager);
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
	void shouldDeleteUserCorrectly() {
		when(userRepository.existsById(1L)).thenReturn(true);
		assertDoesNotThrow(() -> userService.deleteUser(1L));
	}

	@Test
	void shouldAddItemWhenOwnerExists() {
		User owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
		Item savedItem = Item.builder().id(1L).name("Дрель").description("Ударная").available(true).owner(owner).build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

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
		User owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
		Item existingItem = Item.builder().id(1L).name("Дрель").description("Ударная").available(true).owner(owner).build();

		when(itemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
		ItemDto updateData = ItemDto.builder().name("Супер Дрель").build();

		assertThrows(NotFoundException.class, () -> itemService.updateItem(2L, 1L, updateData));
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
