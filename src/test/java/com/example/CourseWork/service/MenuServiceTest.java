package com.example.CourseWork.service;

import com.example.CourseWork.dto.*;
import com.example.CourseWork.mapper.MenuMapper;
import com.example.CourseWork.model.*;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;
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
    private RecommendationService recommendationService;

    @Mock
    private DishRatingService dishRatingService;

    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        menuService = new MenuServiceImpl(menuRepository, menuMapper, recommendationService, dishRatingService);
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

        when(menuRepository.findAll()).thenReturn(List.of(menu));
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

        assertThrows(RuntimeException.class, () -> menuService.updateMenu(menuId, menuDto));
    }
}
