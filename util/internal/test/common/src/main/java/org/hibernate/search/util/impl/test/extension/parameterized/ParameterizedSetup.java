/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks the setup method for a parameterized class tests.
 * Requires an {@link org.junit.jupiter.params.provider.ArgumentsSource argument source},
 * see the JUnit {@link org.junit.jupiter.params.provider built-in ones}, or provide your custom one.
 * <p>
 * An example of a parameterized class test:
 * <pre>{@code
 * @ParameterizedClass
 * class Experiments3Test {
 *
 * 	// Arguments for a parameterized class setup.
 * 	public static List<? extends Arguments> params() {
 * 		// return arguments
 *     }
 *
 * 	// Arguments for a parameterized test.
 * 	public static List<? extends Arguments> testParams() {
 * 		// return arguments
 *     }
 *
 *
 * 	// Setup method that will use the params method to configure the test execution
 * 	// and will run all the @Test/@ParameterizedTest tests within this class.
 *    @ParameterizedSetup
 *    @MethodSource("params")
 *    void env(String string, int number, boolean bool) {
 * 		// configure test class for a set of config arguments
 *    }
 *
 * 	// A simple test.
 * 	// Will be executed once after each class setup execution.
 *    @Test
 *    void test1() {
 * 		// ...
 *    }
 *
 * 	// A parameterized test within a parameterized class.
 * 	// Will be executed for each set of arguments provided by the test parameter source after each class setup execution.
 *    @ParameterizedTest
 *    @MethodSource("testParams")
 *    void test2(String string) {
 * 		// ...
 *    }
 * }
 * }</pre>
 */
@Inherited
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(ParameterizedClassExtension.class)
public @interface ParameterizedSetup {
}
