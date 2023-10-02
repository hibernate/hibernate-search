/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorIdentifier;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoConstructorProjectionDefinitionMessages;

public final class ProjectionConstructorPath {
	private static final PojoConstructorProjectionDefinitionMessages MESSAGES =
			PojoConstructorProjectionDefinitionMessages.INSTANCE;

	private final PojoConstructorIdentifier constructor;
	private final ProjectionConstructorPath child;
	private final int childPosition;

	public ProjectionConstructorPath(PojoConstructorIdentifier constructor,
			ProjectionConstructorPath child, int childPosition) {
		this.constructor = constructor;
		this.child = child;
		this.childPosition = childPosition;
	}

	public ProjectionConstructorPath(PojoConstructorIdentifier constructor) {
		this( constructor, null, -1 );
	}

	public String toPrefixedString() {
		return "\n" + MESSAGES.executedConstructorPath() + "\n" + this;
	}

	@Override
	public String toString() {
		return child == null
				? constructor.toString()
				: child + "\n\t\u2937 for parameter #" + childPosition + " in " + constructor.toHighlightedString(
						childPosition );
	}
}
