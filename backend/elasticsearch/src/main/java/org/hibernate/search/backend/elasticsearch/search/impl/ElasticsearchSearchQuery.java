/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchSearchQuery<T> implements SearchQuery<T> {

	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final ElasticsearchWorkFactory workFactory;
	private final Set<String> indexNames;
	private final JsonObject rootQueryClause;
	private final Function<DocumentReference, T> hitTransformer;

	private Long firstResultIndex;
	private Long maxResultsCount;

	public ElasticsearchSearchQuery(ElasticsearchWorkOrchestrator queryOrchestrator,
			ElasticsearchWorkFactory workFactory,
			Set<String> indexNames, JsonObject rootQueryClause, Function<DocumentReference, T> hitTransformer) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.indexNames = indexNames;
		this.rootQueryClause = rootQueryClause;
		this.hitTransformer = hitTransformer;
	}

	@Override
	public void setFirstResult(Long firstResultIndex) {
		this.firstResultIndex = firstResultIndex;
	}

	@Override
	public void setMaxResults(Long maxResultsCount) {
		this.maxResultsCount = maxResultsCount;
	}

	@Override
	public SearchResult<T> execute() {
		JsonObject payload = new JsonObject();
		payload.add( "query", rootQueryClause );
		ElasticsearchWork<SearchResult<T>> work = workFactory.search(
				indexNames, payload, hitTransformer,
				firstResultIndex, maxResultsCount );
		return queryOrchestrator.submit( work ).join();
	}

}
