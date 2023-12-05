package ru.gb.lesson2.hw;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestProcessor {

  /**
   * Данный метод находит все void методы без аргументов в классе, и запускеет их.
   * <p>
   * Для запуска создается тестовый объект с помощью конструткора без аргументов.
   */
  public static void runTest(Class<?> testClass) {
    final Constructor<?> declaredConstructor;
    try {
      declaredConstructor = testClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
    }

    final Object testObj;
    try {
      testObj = declaredConstructor.newInstance();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
    }

    List<Method> methods = new ArrayList<>();
    for (Method method : testClass.getDeclaredMethods()) {
      // Выбираем методы с аннотацией @Test, при этом пропуская методы с аннотацией @Skip
      if (method.isAnnotationPresent(Test.class) && !method.isAnnotationPresent(Skip.class)) {
        checkTestMethod(method);
        methods.add(method);
      }
    }

    // Запускаем тесты с аннотацией @BeforeEach
    methods.stream().filter(m -> m.isAnnotationPresent(BeforeEach.class)).
            sorted(Comparator.comparingInt(m -> m.getAnnotation(Test.class).order())).
            forEach(m -> runTest(m, testObj));

    // Запускаем тесты в соответствии с @Test(order)
    methods.stream().filter(m -> !m.isAnnotationPresent(AfterEach.class) && !m.isAnnotationPresent(BeforeEach.class)).
            sorted(Comparator.comparingInt(m -> m.getAnnotation(Test.class).order())).
            forEach(m -> runTest(m, testObj));
    // Запускаем тесты с аннотацией @AfterEach
    methods.stream().filter(m -> m.isAnnotationPresent(AfterEach.class)).
            sorted(Comparator.comparingInt(m -> m.getAnnotation(Test.class).order())).
            forEach(m -> runTest(m, testObj));
  }

  private static void checkTestMethod(Method method) {
    if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
      throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
    }
  }

  private static void runTest(Method testMethod, Object testObj) {
    try {
      testMethod.invoke(testObj);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
    } catch (AssertionError e) {

    }
  }

}
