/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneGeoPointFieldAggregationBuilderFactory
		extends AbstractLuceneFieldAggregationBuilderFactory<GeoPoint> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneFieldCodec<GeoPoint> codec;

	public LuceneGeoPointFieldAggregationBuilderFactory(boolean aggregable, LuceneFieldCodec<GeoPoint> codec) {
		super( aggregable );
		this.codec = codec;
	}

	@Override
	public boolean isAggregable() {
		return aggregable;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<GeoPoint> field, Class<K> expectedType, ValueConvert convert) {
		throw log.directValueLookupNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<GeoPoint> field, Class<K> expectedType, ValueConvert convert) {
		throw log.rangesNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	protected LuceneFieldCodec<GeoPoint> getCodec() {
		return codec;
	}
}
