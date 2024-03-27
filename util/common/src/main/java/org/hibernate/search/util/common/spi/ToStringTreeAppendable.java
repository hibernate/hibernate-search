/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.spi;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * Implemented by classes that can be rendered to a string representing a tree.
 */
@Incubating
public interface ToStringTreeAppendable {

	/**
	 * Appends information about {@code this} to the given {@code appender}.
	 * <p>
	 * <strong>WARNING:</strong> This generally shouldn't be called directly, as {@link ToStringTreeAppender}
	 * will automatically call this method for {@link ToStringTreeAppendable} values passed
	 * to {@link ToStringTreeAppender#attribute(String, Object)}/{@link ToStringTreeAppender#value(Object)}.
	 * <p>
	 * Implementations should assume that calls to
	 * {@link ToStringTreeAppender#startObject()}/{@link ToStringTreeAppender#endObject()}
	 * for {@code this} are handled by the caller.
	 *
	 * @param appender A {@link ToStringTreeAppender}.
	 */
	void appendTo(ToStringTreeAppender appender);

	/**
	 * A reasonable implementation of {@link Object#toString()} relying on {@link #appendTo(ToStringTreeAppender)}.
	 * @return A string representation of the given {@code appendable}.
	 */
	default String toStringTree() {
		return new ToStringTreeBuilder( ToStringStyle.inlineDelimiterStructure() ).value( this ).toString();
	}

}
