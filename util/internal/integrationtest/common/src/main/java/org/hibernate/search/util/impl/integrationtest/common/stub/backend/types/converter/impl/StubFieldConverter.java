/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

public class StubFieldConverter<F> {
	private final Class<F> type;
	private final DslConverter<?, ? extends F> dslConverter;
	private final ProjectionConverter<? super F, ?> projectionConverter;

	public StubFieldConverter(Class<F> type,
			DslConverter<?, ? extends F> dslConverter,
			ProjectionConverter<? super F, ?> projectionConverter) {
		this.type = type;
		this.dslConverter = dslConverter;
		this.projectionConverter = projectionConverter;
	}

	public Object convertIndexToProjection(Object indexValue, FromDocumentFieldValueConvertContext context) {
		return projectionConverter.convert( type.cast( indexValue ), context );
	}

	public boolean isConvertIndexToProjectionCompatibleWith(StubFieldConverter<?> other) {
		return projectionConverter.isCompatibleWith( other.projectionConverter );
	}

	public DslConverter<?, ? extends F> getDslConverter() {
		return dslConverter;
	}

	public ProjectionConverter<? super F, ?> getProjectionConverter() {
		return projectionConverter;
	}
}
