/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DistanceSortKey;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.FieldProjectionRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonObject;

/**
 * The context holding all the useful information pertaining to the Elasticsearch search query,
 * to be used:
 * <ul>
 *     <li>When building later parts of the query, to get information on more basic parts of the query.
 *     For example distance projections need to inspect distance sorts (if any) for optimization purposes.
 *     ({@link #getDistanceSortIndex(String, GeoPoint)}</li>
 *     <li>When extracting data from the response, to get an "extract" context linked to the session/loading context
 *     ({@link #createExtractContext(JsonObject)}</li>
 * </ul>
 */
class ElasticsearchSearchQueryRequestContext implements ProjectionRequestContext, AggregationRequestContext {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContext<?, ?> loadingContext;
	private final PredicateRequestContext rootPredicateContext;
	private final Map<DistanceSortKey, Integer> distanceSorts;

	ElasticsearchSearchQueryRequestContext(
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContext<?, ?> loadingContext,
			PredicateRequestContext rootPredicateContext,
			Map<DistanceSortKey, Integer> distanceSorts) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.rootPredicateContext = rootPredicateContext;
		this.distanceSorts = distanceSorts != null ? Collections.unmodifiableMap( distanceSorts ) : null;
	}

	@Override
	public PredicateRequestContext getRootPredicateContext() {
		return rootPredicateContext;
	}

	@Override
	public Integer getDistanceSortIndex(String absoluteFieldPath, GeoPoint location) {
		if ( distanceSorts == null ) {
			return null;
		}

		return distanceSorts.get( new DistanceSortKey( absoluteFieldPath, location ) );
	}

	@Override
	public ElasticsearchSearchSyntax getSearchSyntax() {
		return scope.searchSyntax();
	}

	@Override
	public void checkValidField(String absoluteFieldPath) {
		// All fields are valid at the root.
	}

	@Override
	public ProjectionRequestContext root() {
		return this;
	}

	@Override
	public ProjectionRequestContext forField(String absoluteFieldPath, String[] absoluteFieldPathComponents) {
		return new FieldProjectionRequestContext( this, absoluteFieldPath, absoluteFieldPathComponents );
	}

	@Override
	public String absoluteCurrentFieldPath() {
		return null;
	}

	@Override
	public String[] relativeCurrentFieldPathComponents() {
		return null;
	}

	ElasticsearchSearchQueryExtractContext createExtractContext(JsonObject responseBody) {
		return new ElasticsearchSearchQueryExtractContext(
				this,
				sessionContext,
				loadingContext.createProjectionHitMapper(),
				responseBody
		);
	}

}
