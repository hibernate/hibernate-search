/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks test classes that carry class-level parameters, requiring a
 * <p>
 * Such test classes must have a setup method annotated with {@link ParameterizedSetup}.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(ParameterizedClassLifecycleStateExtension.class)
public @interface ParameterizedClass {
}
