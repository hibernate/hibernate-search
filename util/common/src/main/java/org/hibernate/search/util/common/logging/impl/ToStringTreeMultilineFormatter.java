/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith} to format
 * objects using a {@link ToStringTreeBuilder}.
 */
public final class ToStringTreeMultilineFormatter {

	private final Object object;

	public ToStringTreeMultilineFormatter(Object object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return new ToStringTreeBuilder( ToStringStyle.multilineIndentStructure() )
				.value( object )
				.toString();
	}
}
