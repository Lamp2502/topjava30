package ru.javawebinar.topjava.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit4.SpringRunner;
import ru.javawebinar.topjava.model.Meal;
import ru.javawebinar.topjava.util.exception.NotFoundException;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static ru.javawebinar.topjava.MealTestData.*;
import static ru.javawebinar.topjava.UserTestData.ADMIN_ID;
import static ru.javawebinar.topjava.UserTestData.USER_ID;

@ContextConfiguration({
        "classpath:spring/spring-app.xml",
        "classpath:spring/spring-db.xml"
})
@RunWith(SpringRunner.class)
@Sql(scripts = "classpath:db/populateDB.sql", config = @SqlConfig(encoding = "UTF-8"))
public class MealServiceTest {

    static {
        // Only for postgres driver logging
        // It uses java.util.logging and logged via jul-to-slf4j bridge
        SLF4JBridgeHandler.install();
    }

    @Autowired
    private MealService service;

    @Test
    public void get() {
        Meal actual = service.get(ADMIN_MEAL_ID, ADMIN_ID);
        assertThat(actual).usingRecursiveComparison().isEqualTo(adminMeal1);
    }

    @Test
    public void getNotFound() {
        assertThrows(NotFoundException.class, () -> service.get(NOT_FOUND, USER_ID));
    }

    @Test
    public void getNotOwn() {
        assertThrows(NotFoundException.class, () -> service.get(MEAL1_ID, ADMIN_MEAL_ID));
    }

    @Test
    public void delete() {
        service.delete(MEAL1_ID, USER_ID);
        assertThrows(NotFoundException.class, () -> service.get(MEAL1_ID, USER_ID));
    }

    @Test
    public void deleteNotFound() {
        assertThrows(NotFoundException.class, () -> service.delete(NOT_FOUND, USER_ID));
    }

    @Test
    public void deleteNotOwn() {
        assertThrows(NotFoundException.class, () -> service.delete(MEAL1_ID, ADMIN_MEAL_ID));
    }

    @Test
    public void getBetweenInclusive() {
        assertThat(service.getBetweenInclusive(LocalDate.of(2020, Month.JANUARY, 30),
                LocalDate.of(2020, Month.JANUARY, 30), USER_ID))
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(Arrays.asList(meal3, meal2, meal1));
    }

    @Test
    public void getBetweenWithNullDates() {
        assertMatch(service.getBetweenInclusive(null, null, USER_ID), meals);
    }

    @Test
    public void getAll() {
        List<Meal> actual = service.getAll(USER_ID);
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(meals);
    }

    @Test
    public void update() {
        Meal updated = getUpdated();
        service.update(updated, USER_ID);
        assertThat(service.get(MEAL1_ID, USER_ID))
                .usingRecursiveComparison()
                .isEqualTo(getUpdated());
    }

    @Test
    public void updateNotOwn() {
        assertThrows(NotFoundException.class, () -> service.update(meal1, ADMIN_ID));
        assertThat(service.get(MEAL1_ID, USER_ID))
                .usingRecursiveComparison()
                .isEqualTo(meal1);
    }

    @Test
    public void create() {
        Meal created = service.create(getNew(), USER_ID);
        Meal newMeal = getNew();
        newMeal.setId(created.getId());
        assertThat(created)
                .usingRecursiveComparison()
                .isEqualTo(newMeal);
        // The created object may change, so take it out again.
        assertThat(service.get(created.getId(), USER_ID))
                .usingRecursiveComparison()
                .isEqualTo(newMeal);
    }

    @Test
    public void duplicateDateTimeCreate() {
        Meal mealWithIncorrectDate = new Meal(null, meal1.getDateTime(), "duplicate", 100);
        assertThrows(DataAccessException.class, () ->
                service.create(mealWithIncorrectDate, USER_ID));
    }
}