/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractElasticsearchFieldPredicateBuilderFactory<F>
		implements ElasticsearchFieldPredicateBuilderFactory<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean searchable;

	protected final ElasticsearchFieldCodec<F> codec;

	AbstractElasticsearchFieldPredicateBuilderFactory(boolean searchable, ElasticsearchFieldCodec<F> codec) {
		this.searchable = searchable;
		this.codec = codec;
	}

	@Override
	public boolean isSearchable() {
		return searchable;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldPredicateBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractElasticsearchFieldPredicateBuilderFactory<?> castedOther =
				(AbstractElasticsearchFieldPredicateBuilderFactory<?>) other;
		return searchable == castedOther.searchable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public PhrasePredicateBuilder createPhrasePredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public WildcardPredicateBuilder createWildcardPredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			ElasticsearchSearchFieldContext<F> field) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field) {
		throw log.spatialPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field) {
		throw log.spatialPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field) {
		throw log.spatialPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	protected void checkSearchable(ElasticsearchSearchFieldContext<?> field) {
		if ( !searchable ) {
			throw log.nonSearchableField( field.absolutePath(), field.eventContext() );
		}
	}
}
