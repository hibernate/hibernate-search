/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

public class ElasticsearchTextFieldPredicateBuilderFactory
		extends ElasticsearchStandardFieldPredicateBuilderFactory<String> {

	private final String type;
	private final String analyzer;
	private final String normalizer;

	public ElasticsearchTextFieldPredicateBuilderFactory( boolean searchable,
			DslConverter<?, ? extends String> converter,
			DslConverter<String, ? extends String> rawConverter,
			ElasticsearchFieldCodec<String> codec, PropertyMapping mapping) {
		super( searchable, converter, rawConverter, codec );
		this.type = mapping.getType();
		this.analyzer = ( mapping.getSearchAnalyzer() != null ) ?
				mapping.getSearchAnalyzer() : mapping.getAnalyzer();
		this.normalizer = mapping.getNormalizer();
	}

	@Override
	public boolean hasCompatibleAnalyzer(ElasticsearchFieldPredicateBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}

		ElasticsearchTextFieldPredicateBuilderFactory castedOther = (ElasticsearchTextFieldPredicateBuilderFactory) other;
		return Objects.equals( analyzer, castedOther.analyzer ) && Objects.equals( normalizer, castedOther.normalizer );
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> createMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, ElasticsearchCompatibilityChecker converterChecker,
			ElasticsearchCompatibilityChecker analyzerChecker) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchTextMatchPredicateBuilder( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec, type, analyzerChecker );
	}

	@Override
	public PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> createPhrasePredicateBuilder(
			String absoluteFieldPath, ElasticsearchCompatibilityChecker analyzerChecker) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchTextPhrasePredicateBuilder( absoluteFieldPath, analyzerChecker );
	}

	@Override
	public WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchTextWildcardPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchSimpleQueryStringPredicateBuilderFieldState( absoluteFieldPath );
	}
}
