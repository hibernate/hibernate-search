/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be executed before each test in the {@link ParameterizedClass} test class.
 * <p>
 * Since both {@link org.junit.jupiter.api.BeforeAll} and {@link org.junit.jupiter.api.BeforeEach} will happen prior to the execution
 * of the {@link ParameterizedSetup} this annotation allows to add some logic in after parameterized setup and before each actual test execution.
 */
@Inherited
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterizedSetupBeforeTest {
}
