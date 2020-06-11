/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneDistanceToFieldProjectionBuilder implements DistanceToFieldProjectionBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final LuceneSearchFieldContext<GeoPoint> field;

	private final LuceneFieldCodec<GeoPoint> codec;

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public LuceneDistanceToFieldProjectionBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<GeoPoint> field, LuceneFieldCodec<GeoPoint> codec, GeoPoint center) {
		this.searchContext = searchContext;
		this.field = field;
		this.codec = codec;
		this.center = center;
	}

	@Override
	public DistanceToFieldProjectionBuilder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
		if ( accumulatorProvider.isSingleValued() && field.multiValuedInRoot() ) {
			throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
		}
		return new LuceneDistanceToFieldProjection<>( searchContext.indexes().indexNames(), field.absolutePath(),
				field.nestedDocumentPath(), field.multiValuedInRoot(), codec, center, unit, accumulatorProvider.get() );
	}
}
