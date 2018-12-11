/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserDocumentFieldConverter;

public final class LuceneLocalDateFieldConverter extends AbstractLuceneFieldConverter<LocalDate, Long> {

	public LuceneLocalDateFieldConverter(UserDocumentFieldConverter<LocalDate> userConverter) {
		super( userConverter );
	}

	@Override
	public Long convertDslToIndex(Object value,
			ToDocumentFieldValueConvertContext context) {
		LocalDate rawValue = userConverter.convertDslToIndex( value, context );
		if ( value == null ) {
			return null;
		}
		return rawValue.toEpochDay();
	}
}
