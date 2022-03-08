/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Internal annotation to suppress some JQAssistant rules.
 * <p>
 * Note that rules must be specifically designed to take the annotation into account;
 * not all rules are.
 */
@Documented
@Target(TYPE)
@Retention(CLASS)
public @interface SuppressJQAssistant {

	String reason();

}
