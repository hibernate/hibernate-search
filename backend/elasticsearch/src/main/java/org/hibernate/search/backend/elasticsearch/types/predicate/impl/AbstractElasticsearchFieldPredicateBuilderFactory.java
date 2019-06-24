/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractElasticsearchFieldPredicateBuilderFactory implements ElasticsearchFieldPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean hasCompatibleAnalyzer(ElasticsearchFieldPredicateBuilderFactory other) {
		// analyzers are not involved in a non-text field predicate clause
		return true;
	}

	@Override
	public PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> createPhrasePredicateBuilder(
			String absoluteFieldPath, ElasticsearchCompatibilityChecker analyzerChecker) {
		throw log.textPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath) {
		throw log.textPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		throw log.textPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}
}
