/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

import org.hibernate.search.spi.IndexedTypeIdentifier;

public final class IndexedTypeIdentifierFormatter {

	private final IndexedTypeIdentifier type;

	public IndexedTypeIdentifierFormatter(final IndexedTypeIdentifier type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return type == null ? "null" : type.getName();
	}

}
