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
 * Marks the annotated element as incubating. Usual compatibility policies do not apply:
 * The contract of incubating elements (e.g. types, methods etc.)
 * is under active development and may be incompatibly altered - or removed - in subsequent releases.
 * <p>
 * Usage of incubating API members is encouraged (so the development team can get feedback on these new features)
 * but you should be prepared for updating code which is using them as needed.
 *
 * @author Gunnar Morling
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(CLASS)
public @interface Incubating {
}
