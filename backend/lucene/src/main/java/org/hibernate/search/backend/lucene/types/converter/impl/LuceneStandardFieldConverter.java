/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserDocumentFieldConverter;

public final class LuceneStandardFieldConverter<F> extends AbstractLuceneFieldConverter<F, F> {

	public LuceneStandardFieldConverter(UserDocumentFieldConverter<F> userConverter) {
		super( userConverter );
	}

	@Override
	public F convertDslToIndex(Object value,
			ToDocumentFieldValueConvertContext context) {
		return userConverter.convertDslToIndex( value, context );
	}
}
