/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * @param <F> The field type exposed to the mapper.
 * @see LuceneFieldCodec
 */
public class LuceneStandardFieldProjectionBuilderFactory<F> extends AbstractLuceneFieldProjectionBuilderFactory<F> {

	public LuceneStandardFieldProjectionBuilderFactory(boolean projectable,
			ProjectionConverter<? super F, ?> converter, ProjectionConverter<? super F, F> rawConverter,
			LuceneFieldCodec<F> codec) {
		super( projectable, converter, rawConverter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(Set<String> indexNames,
			String absoluteFieldPath, String nestedDocumentPath, boolean multiValuedFieldInRoot, GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

}
