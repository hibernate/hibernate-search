/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneDistanceToFieldProjectionBuilder implements DistanceToFieldProjectionBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedDocumentPath;
	private final boolean multiValuedFieldInRoot;

	private final LuceneFieldCodec<GeoPoint> codec;

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public LuceneDistanceToFieldProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			String nestedDocumentPath, boolean multiValuedFieldInRoot, LuceneFieldCodec<GeoPoint> codec,
			GeoPoint center) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.multiValuedFieldInRoot = multiValuedFieldInRoot;
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
		if ( accumulatorProvider.isSingleValued() && multiValuedFieldInRoot ) {
			throw log.invalidSingleValuedProjectionOnMultiValuedField( absoluteFieldPath,
					EventContexts.fromIndexNames( indexNames ) );
		}
		return new LuceneDistanceToFieldProjection<>( indexNames, absoluteFieldPath, nestedDocumentPath,
				multiValuedFieldInRoot, codec, center, unit, accumulatorProvider.get() );
	}
}
