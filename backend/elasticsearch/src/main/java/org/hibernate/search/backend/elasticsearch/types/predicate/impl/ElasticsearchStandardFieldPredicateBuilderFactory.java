/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;

public class ElasticsearchStandardFieldPredicateBuilderFactory<F>
		extends AbstractElasticsearchFieldPredicateBuilderFactory {

	protected final ToDocumentFieldValueConverter<?, ? extends F> converter;
	protected final ToDocumentFieldValueConverter<F, ? extends F> rawConverter;

	protected final ElasticsearchFieldCodec<F> codec;

	private final boolean searchable;

	public ElasticsearchStandardFieldPredicateBuilderFactory(boolean searchable,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.searchable = searchable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldPredicateBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldPredicateBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldPredicateBuilderFactory<?>) other;
		return searchable == castedOther.searchable && codec.isCompatibleWith( castedOther.codec );
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
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, ElasticsearchCompatibilityChecker converterChecker,
			ElasticsearchCompatibilityChecker analyzerChecker) {
		return new ElasticsearchStandardMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> createRangePredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, ElasticsearchCompatibilityChecker converterChecker) {
		return new ElasticsearchRangePredicateBuilder<>( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
	}
}
