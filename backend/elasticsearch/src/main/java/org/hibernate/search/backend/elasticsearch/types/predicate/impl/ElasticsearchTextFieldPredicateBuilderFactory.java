/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

public class ElasticsearchTextFieldPredicateBuilderFactory
		extends ElasticsearchStandardFieldPredicateBuilderFactory<String> {

	public ElasticsearchTextFieldPredicateBuilderFactory(
			ToDocumentFieldValueConverter<?, ? extends String> converter,
			ToDocumentFieldValueConverter<String, ? extends String> rawConverter,
			ElasticsearchFieldCodec<String> codec) {
		super( converter, rawConverter, codec );
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> createMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, DslConverter dslConverter) {
		return new ElasticsearchTextMatchPredicateBuilder( searchContext, absoluteFieldPath, getConverter( dslConverter ), codec );
	}

	@Override
	public PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> createPhrasePredicateBuilder(
			String absoluteFieldPath) {
		return new ElasticsearchTextPhrasePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath) {
		return new ElasticsearchTextWildcardPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public ElasticsearchSimpleQueryStringPredicateBuilderFieldContext createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		return new ElasticsearchSimpleQueryStringPredicateBuilderFieldContext( absoluteFieldPath );
	}
}
