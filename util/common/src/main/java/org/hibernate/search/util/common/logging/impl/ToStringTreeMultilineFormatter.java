/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
