/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubIdProjection<I> implements StubSearchProjection<I> {

	private final ProjectionConverter<String, ? extends I> converter;

	StubIdProjection(ProjectionConverter<String, ? extends I> converter) {
		this.converter = converter;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex.next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public I transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		DocumentReference documentReference = (DocumentReference) extractedData;

		context.fromDocumentValueConvertContext();
		return converter.fromDocumentValue( documentReference.id(),
				context.fromDocumentValueConvertContext() );
	}
}
