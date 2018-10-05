/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.FieldSearchProjectionBuilderImpl;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

class ElasticsearchSearchProjectionFactoryImpl implements SearchProjectionFactory<ElasticsearchSearchProjection<?>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;

	private final ElasticsearchSearchTargetModel searchTargetModel;

	ElasticsearchSearchProjectionFactoryImpl(SearchBackendContext searchBackendContext,
			ElasticsearchSearchTargetModel searchTargetModel) {
		this.searchBackendContext = searchBackendContext;
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return searchBackendContext.getDocumentReferenceProjectionBuilder();
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> clazz) {
		return new FieldSearchProjectionBuilderImpl<>( searchTargetModel, absoluteFieldPath, clazz );
	}

	@Override
	public ObjectSearchProjectionBuilder object() {
		return searchBackendContext.getObjectProjectionBuilder();
	}

	@Override
	public ReferenceSearchProjectionBuilder reference() {
		return searchBackendContext.getReferenceProjectionBuilder();
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return searchBackendContext.getScoreProjectionBuilder();
	}

	@Override
	public ElasticsearchSearchProjection<?> toImplementation(SearchProjection<?> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		return (ElasticsearchSearchProjection<?>) projection;
	}

}
