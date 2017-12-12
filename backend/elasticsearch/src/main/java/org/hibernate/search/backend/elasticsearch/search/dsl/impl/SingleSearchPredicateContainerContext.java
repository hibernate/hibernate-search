/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;

import com.google.gson.JsonObject;

public class SingleSearchPredicateContainerContext extends AbstractSearchPredicateContainerContext<SearchPredicate>
		implements ElasticsearchSearchPredicateContributor {

	private ElasticsearchSearchPredicateContributor singlePredicateContributor;
	private JsonObject jsonObjectResult;
	private ElasticsearchSearchPredicate searchPredicateResult;

	public SingleSearchPredicateContainerContext(SearchTargetContext targetContext) {
		super( targetContext );
	}

	@Override
	protected void add(ElasticsearchSearchPredicateContributor child) {
		this.singlePredicateContributor = child;
	}

	@Override
	protected ElasticsearchSearchPredicate getNext() {
		return getSearchPredicateResult();
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		collector.accept( getJsonObjectResult() );
	}

	private ElasticsearchSearchPredicate getSearchPredicateResult() {
		if ( searchPredicateResult == null ) {
			searchPredicateResult = new ElasticsearchSearchPredicate( getJsonObjectResult() );
		}
		return searchPredicateResult;
	}

	private JsonObject getJsonObjectResult() {
		if ( jsonObjectResult == null ) {
			singlePredicateContributor.contribute( this::setJsonObjectResult );
		}
		return jsonObjectResult;
	}

	private void setJsonObjectResult(JsonObject jsonObjectResult) {
		this.jsonObjectResult = jsonObjectResult;
	}
}
