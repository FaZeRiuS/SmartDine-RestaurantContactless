package com.example.CourseWork.service.menu;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.dto.menu.MenuDto;
import com.example.CourseWork.dto.menu.MenuResponseDto;
import com.example.CourseWork.dto.menu.MenuWithDishesDto;
import com.example.CourseWork.mapper.MenuMapper;
import com.example.CourseWork.model.*;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.menu.impl.MenuServiceImpl;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private DishRatingService dishRatingService;

    @Mock
    private DishRepository dishRepository;

    private MenuServiceImpl menuService;
    private Clock testClock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testClock = Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("UTC"));
        menuService = new MenuServiceImpl(
                menuRepository,
                dishRepository,
                menuMapper,
                dishRatingService,
                testClock
        );
    }

    @Test
    void testCreateMenu() {
        MenuDto requestDto = new MenuDto();
        requestDto.setName("Lunch Menu");

        Menu savedMenu = new Menu();
        savedMenu.setId(1);
        savedMenu.setName("Lunch Menu");

        MenuResponseDto responseDto = new MenuResponseDto();
        responseDto.setId(1);
        responseDto.setName("Lunch Menu");

        when(menuRepository.save(any(Menu.class))).thenReturn(savedMenu);
        when(menuMapper.toResponseDto(any(Menu.class))).thenReturn(responseDto);

        MenuResponseDto result = menuService.createMenu(requestDto);

        assertNotNull(result);
        assertEquals("Lunch Menu", result.getName());
        assertEquals(1, result.getId());
    }

    @Test
    void testUpdateMenu() {
        Integer menuId = 1;
        MenuDto menuDto = new MenuDto();
        menuDto.setName("Updated Menu");

        Menu existingMenu = new Menu();
        existingMenu.setId(menuId);
        existingMenu.setName("Lunch Menu");

        MenuResponseDto responseDto = new MenuResponseDto();
        responseDto.setId(menuId);
        responseDto.setName("Updated Menu");

        when(menuRepository.findById(menuId)).thenReturn(Optional.of(existingMenu));
        when(menuRepository.save(any(Menu.class))).thenReturn(existingMenu);
        when(menuMapper.toResponseDto(any(Menu.class))).thenReturn(responseDto);

        MenuResponseDto result = menuService.updateMenu(menuId, menuDto);

        assertNotNull(result);
        assertEquals("Updated Menu", result.getName());
        assertEquals(menuId, result.getId());
    }

    @Test
    void testGetAllMenusWithDishes() {
        Menu menu = new Menu();
        menu.setId(1);
        menu.setName("Lunch Menu");

        Dish dish = new Dish();
        dish.setId(1);
        dish.setName("Pizza");

        menu.setDishes(List.of(dish));

        MenuWithDishesDto responseDto = new MenuWithDishesDto();
        responseDto.setId(1);
        responseDto.setName("Lunch Menu");
        responseDto.setDishes(List.of(new DishResponseDto()));

        when(menuRepository.findAllWithDishes()).thenReturn(List.of(menu));
        when(menuMapper.toMenuWithDishesDto(any(Menu.class))).thenReturn(responseDto);

        List<MenuWithDishesDto> result = menuService.getAllMenusWithDishes();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Lunch Menu", result.getFirst().getName());
        assertEquals(1, result.getFirst().getDishes().size());
        verify(dishRatingService, times(1)).enrichWithRatings(anyList());
    }

    @Test
    void testDeleteMenu() {
        Integer menuId = 1;
        
        when(menuRepository.existsById(menuId)).thenReturn(true);
        
        menuService.deleteMenu(menuId);

        verify(menuRepository, times(1)).deleteById(menuId);
    }

    @Test
    void testUpdateMenu_MenuNotFound() {
        Integer menuId = 1;
        MenuDto menuDto = new MenuDto();
        menuDto.setName("Non-existent Menu");

        when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> menuService.updateMenu(menuId, menuDto));
        assertEquals(ErrorMessages.MENU_NOT_FOUND, ex.getMessage());
    }

    @Test
    void testFilter_ExcludeAllergens_RemovesMatchingDishes() {
        Menu menu = new Menu();
        menu.setId(1);
        menu.setName("Menu");

        DishResponseDto d1 = new DishResponseDto();
        d1.setId(10);
        DishResponseDto d2 = new DishResponseDto();
        d2.setId(20);

        MenuWithDishesDto mapped = new MenuWithDishesDto();
        mapped.setId(1);
        mapped.setName("Menu");
        mapped.setDishes(new ArrayList<>(List.of(d1, d2)));

        when(menuRepository.findAllWithDishes()).thenReturn(List.of(menu));
        when(menuMapper.toMenuWithDishesDto(any(Menu.class))).thenReturn(mapped);

        Dish dish10 = new Dish();
        dish10.setId(10);
        dish10.setTags(List.of());
        Dish dish20 = new Dish();
        dish20.setId(20);
        dish20.setTags(List.of());
        when(dishRepository.findAllByIdWithTags(any())).thenReturn(List.of(dish10, dish20));

        Dish dish10a = new Dish();
        dish10a.setId(10);
        dish10a.setAllergens(List.of("milk"));
        Dish dish20a = new Dish();
        dish20a.setId(20);
        dish20a.setAllergens(List.of());
        when(dishRepository.findAllByIdWithAllergens(any())).thenReturn(List.of(dish10a, dish20a));

        List<MenuWithDishesDto> result = menuService.getActiveMenusWithDishes("all", List.of(), List.of(), List.of("milk"));

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getDishes());
        assertEquals(1, result.getFirst().getDishes().size());
        assertEquals(20, result.getFirst().getDishes().getFirst().getId());
        verify(dishRatingService, times(1)).enrichWithRatings(anyList());
    }

    @Test
    void testFilter_IncludeTags_OrSemantics() {
        Menu menu = new Menu();
        menu.setId(1);
        menu.setName("Menu");

        DishResponseDto d1 = new DishResponseDto();
        d1.setId(10);
        DishResponseDto d2 = new DishResponseDto();
        d2.setId(20);

        MenuWithDishesDto mapped = new MenuWithDishesDto();
        mapped.setId(1);
        mapped.setName("Menu");
        mapped.setDishes(new ArrayList<>(List.of(d1, d2)));

        when(menuRepository.findAllWithDishes()).thenReturn(List.of(menu));
        when(menuMapper.toMenuWithDishesDto(any(Menu.class))).thenReturn(mapped);

        Dish dish10 = new Dish();
        dish10.setId(10);
        dish10.setTags(List.of("meat"));
        Dish dish20 = new Dish();
        dish20.setId(20);
        dish20.setTags(List.of("seafood"));
        when(dishRepository.findAllByIdWithTags(any())).thenReturn(List.of(dish10, dish20));

        Dish dish10a = new Dish();
        dish10a.setId(10);
        dish10a.setAllergens(List.of());
        Dish dish20a = new Dish();
        dish20a.setId(20);
        dish20a.setAllergens(List.of());
        when(dishRepository.findAllByIdWithAllergens(any())).thenReturn(List.of(dish10a, dish20a));

        List<MenuWithDishesDto> result = menuService.getActiveMenusWithDishes("all", List.of("seafood", "vegan"), List.of(), List.of());

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getDishes().size());
        assertEquals(20, result.getFirst().getDishes().getFirst().getId());
    }

    @Test
    void testFilter_ExcludeTags_RemovesMatchingDishes() {
        Menu menu = new Menu();
        menu.setId(1);
        menu.setName("Menu");

        DishResponseDto d1 = new DishResponseDto();
        d1.setId(10);
        DishResponseDto d2 = new DishResponseDto();
        d2.setId(20);

        MenuWithDishesDto mapped = new MenuWithDishesDto();
        mapped.setId(1);
        mapped.setName("Menu");
        mapped.setDishes(new ArrayList<>(List.of(d1, d2)));

        when(menuRepository.findAllWithDishes()).thenReturn(List.of(menu));
        when(menuMapper.toMenuWithDishesDto(any(Menu.class))).thenReturn(mapped);

        Dish dish10 = new Dish();
        dish10.setId(10);
        dish10.setTags(List.of("alcohol", "drink"));
        Dish dish20 = new Dish();
        dish20.setId(20);
        dish20.setTags(List.of("drink"));
        when(dishRepository.findAllByIdWithTags(any())).thenReturn(List.of(dish10, dish20));

        Dish dish10a = new Dish();
        dish10a.setId(10);
        dish10a.setAllergens(List.of());
        Dish dish20a = new Dish();
        dish20a.setId(20);
        dish20a.setAllergens(List.of());
        when(dishRepository.findAllByIdWithAllergens(any())).thenReturn(List.of(dish10a, dish20a));

        List<MenuWithDishesDto> result = menuService.getActiveMenusWithDishes("all", List.of(), List.of("alcohol"), List.of());

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getDishes().size());
        assertEquals(20, result.getFirst().getDishes().getFirst().getId());
    }
}
