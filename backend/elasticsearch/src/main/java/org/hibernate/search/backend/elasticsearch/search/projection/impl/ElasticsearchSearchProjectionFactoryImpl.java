/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class ElasticsearchSearchProjectionFactoryImpl implements SearchProjectionFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	private final ElasticsearchSearchTargetModel searchTargetModel;

	public ElasticsearchSearchProjectionFactoryImpl(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchTargetModel searchTargetModel) {
		this.searchProjectionBackendContext = searchProjectionBackendContext;
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return searchProjectionBackendContext.getDocumentReferenceProjectionBuilder();
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> clazz) {
		return new FieldSearchProjectionBuilderImpl<>( searchTargetModel, absoluteFieldPath, clazz );
	}

	@Override
	public ObjectSearchProjectionBuilder object() {
		return searchProjectionBackendContext.getObjectProjectionBuilder();
	}

	@Override
	public ReferenceSearchProjectionBuilder reference() {
		return searchProjectionBackendContext.getReferenceProjectionBuilder();
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return searchProjectionBackendContext.getScoreProjectionBuilder();
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return new DistanceToFieldSearchProjectionBuilderImpl( searchTargetModel, absoluteFieldPath, center );
	}

	public ElasticsearchSearchProjection<?> toImplementation(SearchProjection<?> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		return (ElasticsearchSearchProjection<?>) projection;
	}

}
