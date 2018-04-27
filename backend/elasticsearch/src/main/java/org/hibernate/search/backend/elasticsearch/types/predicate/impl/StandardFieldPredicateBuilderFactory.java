/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.MatchPredicateBuilderImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.RangePredicateBuilderImpl;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;


public class StandardFieldPredicateBuilderFactory implements ElasticsearchFieldPredicateBuilderFactory {

	private final ElasticsearchFieldCodec codec;

	public StandardFieldPredicateBuilderFactory(ElasticsearchFieldCodec codec) {
		this.codec = codec;
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateCollector> createMatchPredicateBuilder(String absoluteFieldPath) {
		return new MatchPredicateBuilderImpl( absoluteFieldPath, codec );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateCollector> createRangePredicateBuilder(String absoluteFieldPath) {
		return new RangePredicateBuilderImpl( absoluteFieldPath, codec );
	}
}
