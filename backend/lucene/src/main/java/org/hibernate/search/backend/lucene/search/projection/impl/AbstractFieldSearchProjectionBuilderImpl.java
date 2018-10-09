/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractFieldSearchProjectionBuilderImpl<N, T> implements SearchProjectionBuilder<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchTargetModel searchTargetModel;

	private final String absoluteFieldPath;

	private final Class<N> type;

	protected AbstractFieldSearchProjectionBuilderImpl(LuceneSearchTargetModel searchTargetModel,
			String absoluteFieldPath,
			Class<N> type) {
		this.searchTargetModel = searchTargetModel;
		this.absoluteFieldPath = absoluteFieldPath;
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SearchProjection<T> build() {
		if ( searchTargetModel.getIndexModels().size() == 1 ) {
			LuceneIndexModel indexModel = searchTargetModel.getIndexModels().iterator().next();
			LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

			if ( schemaNode == null ) {
				throw log.invalidProjectionUnknownField( absoluteFieldPath, searchTargetModel.getIndexesEventContext() );
			}

			if ( !schemaNode.getConverter().isProjectionCompatibleWith( type ) ) {
				throw log.invalidProjectionInvalidType( absoluteFieldPath, type,
						searchTargetModel.getIndexesEventContext() );
			}

			return createProjection( (LuceneIndexSchemaFieldNode<N>) schemaNode );
		}

		Map<String, LuceneSearchProjection<T>> projectionsByIndex = CollectionHelper
				.newLinkedHashMap( searchTargetModel.getIndexModels().size() );
		boolean projectionFound = false;

		for ( LuceneIndexModel indexModel : searchTargetModel.getIndexModels() ) {
			LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

			if ( schemaNode != null ) {
				if ( !schemaNode.getConverter().isProjectionCompatibleWith( type ) ) {
					throw log.invalidProjectionInvalidType( absoluteFieldPath, type,
							searchTargetModel.getIndexesEventContext() );
				}

				projectionsByIndex.put( indexModel.getIndexName(),
						createProjection( (LuceneIndexSchemaFieldNode<N>) schemaNode ) );
				projectionFound = true;
			}
			else {
				projectionsByIndex.put( indexModel.getIndexName(), NullSearchProjectionImpl.get() );
			}
		}

		if ( !projectionFound ) {
			throw log.invalidProjectionUnknownField( absoluteFieldPath, searchTargetModel.getIndexesEventContext() );
		}

		return new IndexSensitiveSearchProjectionImpl<>( projectionsByIndex );
	}

	protected abstract LuceneSearchProjection<T> createProjection(LuceneIndexSchemaFieldNode<N> schemaNode);
}
