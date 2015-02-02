/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Custom filter cache keys are a deprecated feature scheduled to be removed in Hibernate Search 6. As of
 * Hibernate Search 5.1, filter cache keys will be determinated automatically based on the filter parameters. Custom
 * filter cache key methods should therefore be removed.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
@Deprecated
public @interface Key {
}
