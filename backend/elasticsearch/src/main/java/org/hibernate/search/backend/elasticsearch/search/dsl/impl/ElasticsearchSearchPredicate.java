/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.JsonObject;

class ElasticsearchSearchPredicate implements SearchPredicate, ElasticsearchSearchPredicateContributor {

	private static Log log = LoggerFactory.make( Log.class );

	public static ElasticsearchSearchPredicate cast(SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		return (ElasticsearchSearchPredicate) predicate;
	}

	private final JsonObject jsonObject;

	ElasticsearchSearchPredicate(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		collector.accept( jsonObject );
	}

}
