/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.highlighter.impl.ElasticsearchSearchHighlighter;
import org.hibernate.search.backend.elasticsearch.search.highlighter.impl.ElasticsearchSearchHighlighterImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DistanceSortKey;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.FieldProjectionRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestRootContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.query.spi.QueryParameters;
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
class ElasticsearchSearchQueryRequestContext implements ProjectionRequestRootContext, AggregationRequestContext {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final PredicateRequestContext rootPredicateContext;
	private final Map<DistanceSortKey, Integer> distanceSorts;
	private final Map<String, ElasticsearchSearchHighlighter> namedHighlighters;
	private final ElasticsearchSearchHighlighter queryHighlighter;
	private final QueryParameters parameters;

	ElasticsearchSearchQueryRequestContext(
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContext<?> loadingContext,
			PredicateRequestContext rootPredicateContext,
			Map<DistanceSortKey, Integer> distanceSorts,
			Map<String, ElasticsearchSearchHighlighter> namedHighlighters,
			ElasticsearchSearchHighlighter queryHighlighter, QueryParameters parameters) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.rootPredicateContext = rootPredicateContext;
		this.distanceSorts = distanceSorts != null ? Collections.unmodifiableMap( distanceSorts ) : null;
		this.namedHighlighters = namedHighlighters;
		this.queryHighlighter = queryHighlighter;
		this.parameters = parameters;
	}

	@Override
	public PredicateRequestContext getRootPredicateContext() {
		return rootPredicateContext;
	}

	@Override
	public boolean isRootContext() {
		return true;
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
	public void checkNotNested(SearchQueryElementTypeKey<?> projectionKey, String hint) {
		// root is not nested
	}

	@Override
	public ProjectionRequestRootContext root() {
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

	@Override
	public NamedValues queryParameters() {
		return parameters;
	}

	@Override
	public boolean projectionCardinalityCorrectlyAddressed(String requiredContextAbsoluteFieldPath) {
		return requiredContextAbsoluteFieldPath == null;
	}

	@Override
	public ElasticsearchSearchHighlighter highlighter(String highlighterName) {
		if ( highlighterName == null ) {
			return ElasticsearchSearchHighlighterImpl.NO_OPTIONS_CONFIGURATION;
		}
		ElasticsearchSearchHighlighter highlighter = namedHighlighters.get( highlighterName );
		if ( highlighter == null ) {
			throw QueryLog.INSTANCE.cannotFindHighlighter( highlighterName, namedHighlighters.keySet() );
		}
		return highlighter;
	}

	@Override
	public ElasticsearchSearchHighlighter queryHighlighter() {
		return queryHighlighter;
	}

	@Override
	public boolean isCompatibleHighlighter(String highlighterName, ProjectionCollector.Provider<?, ?> collectorProvider) {
		ElasticsearchSearchHighlighter highlighter = highlighter( highlighterName );
		if ( ElasticsearchSearchHighlighterImpl.NO_OPTIONS_CONFIGURATION == highlighter ) {
			// if there was no highlighter configured at all it means that the settings are default,
			// and we assume that they are incompatible with the single-valued collector:
			return queryHighlighter != null
					? queryHighlighter.isCompatible( collectorProvider )
					: !collectorProvider.isSingleValued();
		}
		else {
			return highlighter.isCompatible( collectorProvider );
		}
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
