/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.formatter.impl;

import java.time.LocalDate;

public final class LocalDateFieldFormatter implements LuceneFieldFormatter<Long> {

	public static final LocalDateFieldFormatter INSTANCE = new LocalDateFieldFormatter();

	private LocalDateFieldFormatter() {
	}

	@Override
	public Long format(Object value) {
		if ( value == null ) {
			return null;
		}

		return ((LocalDate) value).toEpochDay();
	}
}
