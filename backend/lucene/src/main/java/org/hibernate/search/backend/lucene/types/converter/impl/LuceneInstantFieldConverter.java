/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import java.time.Instant;

import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;

public final class LuceneInstantFieldConverter extends AbstractLuceneFieldConverter<Instant, Long> {

	public LuceneInstantFieldConverter(UserIndexFieldConverter<Instant> userConverter) {
		super( userConverter );
	}

	@Override
	public Long convertDslToIndex(Object value,
			ToIndexFieldValueConvertContext context) {
		Instant rawValue = userConverter.convertDslToIndex( value, context );
		if ( value == null ) {
			return null;
		}
		return rawValue.toEpochMilli();
	}
}
