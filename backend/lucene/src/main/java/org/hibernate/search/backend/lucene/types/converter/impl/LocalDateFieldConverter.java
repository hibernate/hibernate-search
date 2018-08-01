/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import java.time.LocalDate;

public final class LocalDateFieldConverter implements LuceneFieldConverter<Long> {

	public static final LocalDateFieldConverter INSTANCE = new LocalDateFieldConverter();

	private LocalDateFieldConverter() {
	}

	@Override
	public Long convertFromDsl(Object value) {
		if ( value == null ) {
			return null;
		}

		return ((LocalDate) value).toEpochDay();
	}
}
