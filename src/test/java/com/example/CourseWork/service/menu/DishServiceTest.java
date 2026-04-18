package com.example.CourseWork.service.menu;

import com.example.CourseWork.dto.menu.DishDto;
import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.service.menu.impl.DishServiceImpl;
import com.example.CourseWork.service.recommendation.RecommendationService;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("null")
class DishServiceTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private DishMapper dishMapper;

    @Mock
    private DishRatingService dishRatingService;

    @Mock
    private RecommendationService recommendationService;

    private DishService dishService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dishService = new DishServiceImpl(dishRepository, menuRepository, dishMapper, dishRatingService, recommendationService);
    }

    @Test
    void testGetDishById_DishExists() {
        Menu menu = new Menu();
        menu.setId(1);
        menu.setName("Italian Menu");

        Dish dish = new Dish();
        dish.setId(1);
        dish.setName("Pizza");
        dish.setDescription("Delicious pizza with cheese");
        dish.setPrice(new BigDecimal("9.99"));
        dish.setIsAvailable(true);
        dish.setMenus(List.of(menu));

        DishResponseDto responseDto = new DishResponseDto();
        responseDto.setId(1);
        responseDto.setName("Pizza");
        responseDto.setDescription("Delicious pizza with cheese");
        responseDto.setPrice(new BigDecimal("9.99"));
        responseDto.setIsAvailable(true);
        responseDto.setMenuIds(List.of(1));

        Mockito.when(dishRepository.findById(1)).thenReturn(Optional.of(dish));
        Mockito.when(dishMapper.toResponseDto(any(Dish.class))).thenReturn(responseDto);

        DishResponseDto result = dishService.getDishById(1);

        assertNotNull(result);
        assertEquals("Pizza", result.getName());
        assertTrue(result.getMenuIds().contains(1));
        verify(dishRatingService, times(1)).enrichWithRatings(anyList());
    }

    @Test
    void testGetDishById_DishNotFound() {
        Mockito.when(dishRepository.findById(1)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> dishService.getDishById(1));
        assertEquals(ErrorMessages.DISH_NOT_FOUND, ex.getMessage());
    }

    @Test
    void testGetAllDishes() {
        Dish dish1 = new Dish();
        dish1.setIsAvailable(true);

        Menu menu = new Menu();
        menu.setId(1);
        dish1.setMenus(List.of(menu));

        Dish dish2 = new Dish();
        dish2.setIsAvailable(true);
        dish2.setMenus(List.of(menu));

        List<Dish> dishes = Arrays.asList(dish1, dish2);

        DishResponseDto responseDto1 = new DishResponseDto();
        responseDto1.setId(1);
        responseDto1.setMenuIds(List.of(1));
        responseDto1.setIsAvailable(true);

        DishResponseDto responseDto2 = new DishResponseDto();
        responseDto2.setId(2);
        responseDto2.setMenuIds(List.of(1));
        responseDto2.setIsAvailable(true);

        Mockito.when(dishRepository.findByIsAvailableTrue()).thenReturn(dishes);
        Mockito.when(dishMapper.toResponseDto(dish1)).thenReturn(responseDto1);
        Mockito.when(dishMapper.toResponseDto(dish2)).thenReturn(responseDto2);

        List<DishResponseDto> result = dishService.getAllAvailableDishes();

        assertEquals(2, result.size());
        verify(dishRatingService, times(1)).enrichWithRatings(anyList());
    }

    @Test
    void testCreateDish() {
        DishDto dishDto = new DishDto();
        dishDto.setName("Pasta");
        dishDto.setDescription("Delicious pasta with tomato sauce");
        dishDto.setPrice(new BigDecimal("12.99"));
        dishDto.setIsAvailable(true);
        dishDto.setMenuIds(List.of(1));

        Menu menu = new Menu();
        menu.setId(1);
        menu.setName("Italian Menu");

        Dish dishEntity = new Dish();
        dishEntity.setId(1);
        dishEntity.setName("Pasta");
        dishEntity.setDescription("Delicious pasta with tomato sauce");
        dishEntity.setPrice(new BigDecimal("12.99"));
        dishEntity.setIsAvailable(true);
        dishEntity.setMenus(List.of(menu));

        DishResponseDto responseDto = new DishResponseDto();
        responseDto.setId(1);
        responseDto.setName("Pasta");
        responseDto.setDescription("Delicious pasta with tomato sauce");
        responseDto.setPrice(new BigDecimal("12.99"));
        responseDto.setIsAvailable(true);
        responseDto.setMenuIds(List.of(1));

        Mockito.when(dishRepository.save(Mockito.any(Dish.class))).thenReturn(dishEntity);
        Mockito.when(menuRepository.findAllById(dishDto.getMenuIds())).thenReturn(List.of(menu));
        Mockito.when(dishMapper.toResponseDto(any(Dish.class))).thenReturn(responseDto);

        DishResponseDto result = dishService.createDish(dishDto);

        assertNotNull(result);
        assertEquals("Pasta", result.getName());
        assertEquals("Delicious pasta with tomato sauce", result.getDescription());
        assertThat(result.getPrice()).isEqualByComparingTo("12.99");
        assertTrue(result.getIsAvailable());
        assertTrue(result.getMenuIds().contains(1));
    }

    @Test
    void testDeleteDish() {
        Integer dishId = 1;
        Mockito.doNothing().when(dishRepository).deleteById(dishId);

        dishService.deleteDish(dishId);

        Mockito.verify(dishRepository, Mockito.times(1)).deleteById(dishId);
    }

    @Test
    void getPopularDishesForHome_respectsMaxTwoPerMenu() {
        when(dishRepository.findAvailableDishesOrderedByOrderVolume(anyInt()))
                .thenReturn(List.of(new Object[]{1, 100L}, new Object[]{2, 90L}, new Object[]{3, 80L}));
        when(dishRepository.findDishMenuLinksForDishIds(anyList()))
                .thenReturn(List.of(
                        new Object[]{1, 10},
                        new Object[]{2, 10},
                        new Object[]{3, 10}
                ));

        Dish d1 = new Dish();
        d1.setId(1);
        d1.setName("A");
        d1.setIsAvailable(true);
        Dish d2 = new Dish();
        d2.setId(2);
        d2.setName("B");
        d2.setIsAvailable(true);
        when(dishRepository.findAllById(List.of(1, 2))).thenReturn(List.of(d1, d2));
        when(dishMapper.toResponseDto(any(Dish.class))).thenAnswer(invocation -> {
            Dish d = invocation.getArgument(0);
            DishResponseDto dto = new DishResponseDto();
            dto.setId(d.getId());
            dto.setName(d.getName());
            return dto;
        });
        doNothing().when(dishRatingService).enrichWithRatings(anyList());

        List<DishResponseDto> popular = dishService.getPopularDishesForHome(6, 2, Set.of());

        assertThat(popular).extracting(DishResponseDto::getId).containsExactly(1, 2);
        verify(dishRepository).findAllById(List.of(1, 2));
    }

    @Test
    void getPopularDishesForHome_excludesIds() {
        when(dishRepository.findAvailableDishesOrderedByOrderVolume(anyInt()))
                .thenReturn(List.of(new Object[]{1, 100}, new Object[]{2, 90}));
        when(dishRepository.findDishMenuLinksForDishIds(anyList()))
                .thenReturn(List.of(new Object[]{1, 5}, new Object[]{2, 5}));
        Dish d2 = new Dish();
        d2.setId(2);
        d2.setName("B");
        d2.setIsAvailable(true);
        when(dishRepository.findAllById(List.of(2))).thenReturn(List.of(d2));
        when(dishMapper.toResponseDto(d2)).thenAnswer(invocation -> {
            DishResponseDto dto = new DishResponseDto();
            dto.setId(2);
            dto.setName("B");
            return dto;
        });
        doNothing().when(dishRatingService).enrichWithRatings(anyList());

        List<DishResponseDto> popular = dishService.getPopularDishesForHome(6, 2, Set.of(1));

        assertThat(popular).extracting(DishResponseDto::getId).containsExactly(2);
    }
}
