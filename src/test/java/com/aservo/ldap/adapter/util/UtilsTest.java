package com.aservo.ldap.adapter.util;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtilsTest {

    @Test
    @Order(1)
    @DisplayName("it should be able to provide execution of cartesian product")
    public void test001()
            throws Exception {

        List<List<String>> testResult =
                Utils.cartesianProduct(
                        Arrays.asList(Arrays.asList("Apple", "Banana"),
                                Arrays.asList("Red", "Green", "Yellow")
                        ));

        List<List<String>> expectedResult =
                Arrays.asList(
                        Arrays.asList("Apple", "Red"),
                        Arrays.asList("Apple", "Green"),
                        Arrays.asList("Apple", "Yellow"),
                        Arrays.asList("Banana", "Red"),
                        Arrays.asList("Banana", "Green"),
                        Arrays.asList("Banana", "Yellow"));

        Assertions.assertEquals(testResult.size(), expectedResult.size());

        for (int i = 0; i < testResult.size(); i++)
            Assertions.assertArrayEquals(testResult.get(i).toArray(), expectedResult.get(i).toArray());
    }
}
