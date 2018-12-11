/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserDocumentFieldConverter;

public class StubFieldConverter<F> {
	private final Class<F> type;
	private final UserDocumentFieldConverter<F> userConverter;

	public StubFieldConverter(Class<F> type, UserDocumentFieldConverter<F> userConverter) {
		this.type = type;
		this.userConverter = userConverter;
	}

	public Object convertIndexToProjection(Object indexValue, FromDocumentFieldValueConvertContext context) {
		return userConverter.convertIndexToProjection( type.cast( indexValue ), context );
	}

	public boolean isConvertIndexToProjectionCompatibleWith(StubFieldConverter<?> other) {
		return userConverter.isConvertIndexToProjectionCompatibleWith( other.userConverter );
	}
}
