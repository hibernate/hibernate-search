/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;


public class FieldSearchProjectionBuilderImpl<T> implements FieldSearchProjectionBuilder<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchTargetModel searchTargetModel;

	private final String absoluteFieldPath;

	private final Class<T> type;

	public FieldSearchProjectionBuilderImpl(ElasticsearchSearchTargetModel searchTargetModel,
			String absoluteFieldPath,
			Class<T> type) {
		this.searchTargetModel = searchTargetModel;
		this.absoluteFieldPath = absoluteFieldPath;
		this.type = type;
	}

	@Override
	public SearchProjection<T> build() {
		if ( searchTargetModel.getIndexModels().size() == 1 ) {
			ElasticsearchIndexModel indexModel = searchTargetModel.getIndexModels().iterator().next();
			ElasticsearchIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

			if ( schemaNode == null ) {
				throw log.invalidProjectionUnknownField( absoluteFieldPath, searchTargetModel.getIndexesEventContext() );
			}

			if ( !schemaNode.getConverter().isProjectionCompatibleWith( type ) ) {
				throw log.invalidProjectionInvalidType( absoluteFieldPath, type,
						searchTargetModel.getIndexesEventContext() );
			}

			return new FieldSearchProjectionImpl<>( absoluteFieldPath, schemaNode.getConverter() );
		}

		Map<String, ElasticsearchSearchProjection<T>> projectionsByIndex = CollectionHelper
				.newLinkedHashMap( searchTargetModel.getIndexModels().size() );
		boolean projectionFound = false;

		for ( ElasticsearchIndexModel indexModel : searchTargetModel.getIndexModels() ) {
			ElasticsearchIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

			ElasticsearchSearchProjection<T> projection;

			if ( schemaNode != null ) {
				if ( !schemaNode.getConverter().isProjectionCompatibleWith( type ) ) {
					throw log.invalidProjectionInvalidType( absoluteFieldPath, type,
							searchTargetModel.getIndexesEventContext() );
				}

				projection = new FieldSearchProjectionImpl<>( absoluteFieldPath, schemaNode.getConverter() );
				projectionFound = true;
			}
			else {
				projection = NullSearchProjectionImpl.get();
			}

			projectionsByIndex.put( indexModel.getElasticsearchIndexName().original, projection );
		}

		if ( !projectionFound ) {
			throw log.invalidProjectionUnknownField( absoluteFieldPath, searchTargetModel.getIndexesEventContext() );
		}

		return new IndexSensitiveSearchProjectionImpl<>( projectionsByIndex );
	}
}
