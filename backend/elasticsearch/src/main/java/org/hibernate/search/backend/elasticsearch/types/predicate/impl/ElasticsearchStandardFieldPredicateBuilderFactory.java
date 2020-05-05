/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;

public class ElasticsearchStandardFieldPredicateBuilderFactory<F>
		extends AbstractElasticsearchFieldPredicateBuilderFactory<F> {

	protected final DslConverter<?, ? extends F> converter;
	protected final DslConverter<F, ? extends F> rawConverter;

	public ElasticsearchStandardFieldPredicateBuilderFactory(boolean searchable,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			ElasticsearchFieldCodec<F> codec) {
		super( searchable, codec );
		this.converter = converter;
		this.rawConverter = rawConverter;
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldPredicateBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldPredicateBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldPredicateBuilderFactory<?>) other;
		return converter.isCompatibleWith( castedOther.converter );
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> createMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			ElasticsearchCompatibilityChecker converterChecker, ElasticsearchCompatibilityChecker analyzerChecker) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchStandardMatchPredicateBuilder<>(
				searchContext, absoluteFieldPath, nestedPathHierarchy,
				converter, rawConverter, converterChecker,
				codec );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> createRangePredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy
			, ElasticsearchCompatibilityChecker converterChecker) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchRangePredicateBuilder<>(
				searchContext, absoluteFieldPath, nestedPathHierarchy,
				converter, rawConverter, converterChecker,
				codec );
	}

}
