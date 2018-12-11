/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;

public class StubFieldConverter<F> {
	private final Class<F> type;
	private final ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private final FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter;

	public StubFieldConverter(Class<F> type,
			ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter,
			FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter) {
		this.type = type;
		this.dslToIndexConverter = dslToIndexConverter;
		this.indexToProjectionConverter = indexToProjectionConverter;
	}

	public Object convertIndexToProjection(Object indexValue, FromDocumentFieldValueConvertContext context) {
		return indexToProjectionConverter.convert( type.cast( indexValue ), context );
	}

	public boolean isConvertIndexToProjectionCompatibleWith(StubFieldConverter<?> other) {
		return indexToProjectionConverter.isCompatibleWith( other.indexToProjectionConverter );
	}
}
