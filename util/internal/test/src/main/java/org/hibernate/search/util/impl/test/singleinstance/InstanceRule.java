/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.singleinstance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A mix between {@link org.junit.ClassRule} and {@link org.junit.Rule}:
 * it can be applied to instance fields,
 * but the rule will only be executed once for all test methods.
 * <p>
 * Especially useful for parameterized tests.
 * <p>
 * <strong>CAUTION:</strong> this should only be used if test methods are read-only,
 * otherwise one test could have side-effects on others.
 * <p>
 * <strong>WARNING:</strong> this only works with
 * {@link SingleInstanceRunnerWithParameters}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface InstanceRule {
}
