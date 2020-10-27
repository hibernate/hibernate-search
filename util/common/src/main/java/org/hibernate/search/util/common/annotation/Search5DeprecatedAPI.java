/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks the annotated element as still present in Hibernate Search 5 APIs.
 * <p>
 * Which means that the deprecated API still need to be kept in Search 6 APIs.
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(CLASS)
public @interface Search5DeprecatedAPI {
}
