/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An appender for use in {@link ToStringTreeAppendable}.
 */
@Incubating
public interface ToStringTreeAppender {

	/**
	 * Appends a nested, named attribute.
	 * @param name The name of the attribute to append.
	 * @param value The value of the attribute to append.
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender attribute(String name, Object value);

	/**
	 * Appends a nested, unnamed value.
	 * @param value The value to append.
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender value(Object value);

	/**
	 * Starts a nested, unnamed object.
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender startObject();

	/**
	 * Starts a nested, named object.
	 * @param name The name of the object (type, ...).
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender startObject(String name);

	/**
	 * Ends a nested object.
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender endObject();

	/**
	 * Starts a nested, unnamed list.
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender startList();

	/**
	 * Starts a nested, named object.
	 * @param name The name of the list (type, ...).
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender startList(String name);

	/**
	 * Ends a nested list.
	 * @return {@code this}, for method chaining.
	 */
	ToStringTreeAppender endList();

}
