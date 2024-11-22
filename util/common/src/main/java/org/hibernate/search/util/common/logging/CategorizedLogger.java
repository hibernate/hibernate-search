/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a Logger interface that may produce log messages and has to be added to a report in the reference documentation.
 * If annotated logger actually contain any log messages, the {@link CategorizedLogger#description()} is mandatory.
 */
@Target(TYPE)
@Retention(CLASS)
public @interface CategorizedLogger {
	/**
	 * Name of the logging category.
	 * All categories should start with {@code org.hibernate.search.} prefix.
	 * Also make sure that categories extend each other and, if needed, have a "module" suffix.
	 * E.g. having a common category defined in a pojo mapper as {@code org.hibernate.search.something.mapper}
	 * a corresponding category in the ORM mapper would be {@code org.hibernate.search.something.mapper.orm}.
	 * This way should allow enabling all "sub-categories" by enabling logging for a "parent" category.
	 */
	String category();

	/**
	 * Description of a specific logging category.
	 * Goes into reference documentation report.
	 * Try to keep it vague enough to not update it each time a logger changes.
	 */
	String description() default "";

}
