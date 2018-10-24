/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
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

public class LuceneSearchProjectionFactoryImpl implements SearchProjectionFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchTargetModel searchTargetModel;

	public LuceneSearchProjectionFactoryImpl(LuceneSearchTargetModel searchTargetModel) {
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return DocumentReferenceSearchProjectionBuilderImpl.get();
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType) {
		LuceneIndexSchemaFieldNode<?> schemaNode = searchTargetModel.getSchemaNode( absoluteFieldPath );

		return (FieldSearchProjectionBuilder<T>) schemaNode.getProjectionBuilderFactory()
				.createFieldValueProjectionBuilder( absoluteFieldPath, expectedType );
	}

	@Override
	public ObjectSearchProjectionBuilder object() {
		return ObjectSearchProjectionBuilderImpl.get();
	}

	@Override
	public ReferenceSearchProjectionBuilder reference() {
		return ReferenceSearchProjectionBuilderImpl.get();
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return ScoreSearchProjectionBuilderImpl.get();
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		LuceneIndexSchemaFieldNode<?> schemaNode = searchTargetModel.getSchemaNode( absoluteFieldPath );

		return schemaNode.getProjectionBuilderFactory()
				.createDistanceProjectionBuilder( absoluteFieldPath, center );
	}

	public LuceneSearchProjection<?> toImplementation(SearchProjection<?> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		return (LuceneSearchProjection<?>) projection;
	}

}
