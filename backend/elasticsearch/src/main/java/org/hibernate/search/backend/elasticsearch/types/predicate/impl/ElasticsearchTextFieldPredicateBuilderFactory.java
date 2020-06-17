/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

public class ElasticsearchTextFieldPredicateBuilderFactory
		extends ElasticsearchStandardFieldPredicateBuilderFactory<String> {

	private final String type;

	public ElasticsearchTextFieldPredicateBuilderFactory(boolean searchable,
			ElasticsearchFieldCodec<String> codec, PropertyMapping mapping) {
		super( searchable, codec );
		this.type = mapping.getType();
	}

	@Override
	public MatchPredicateBuilder createMatchPredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<String> field) {
		checkSearchable( field );
		return new ElasticsearchTextMatchPredicateBuilder( searchContext, field, codec, type );
	}

	@Override
	public PhrasePredicateBuilder createPhrasePredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<String> field) {
		checkSearchable( field );
		return new ElasticsearchTextPhrasePredicateBuilder( searchContext, field );
	}

	@Override
	public WildcardPredicateBuilder createWildcardPredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<String> field) {
		checkSearchable( field );
		return new ElasticsearchTextWildcardPredicateBuilder( searchContext, field );
	}

	@Override
	public ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			ElasticsearchSearchFieldContext<String> field) {
		checkSearchable( field );
		return new ElasticsearchSimpleQueryStringPredicateBuilderFieldState( field );
	}
}
