/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;

public class FieldSearchProjectionBuilderImpl<T> extends AbstractFieldSearchProjectionBuilderImpl<T, T>
		implements FieldSearchProjectionBuilder<T> {

	public FieldSearchProjectionBuilderImpl(LuceneSearchTargetModel searchTargetModel,
			String absoluteFieldPath,
			Class<T> type) {
		super( searchTargetModel, absoluteFieldPath, type );
	}

	@Override
	protected LuceneSearchProjection<T> createProjection(LuceneIndexSchemaFieldNode<T> schemaNode) {
		return new FieldSearchProjectionImpl<>( (LuceneIndexSchemaFieldNode<T>) schemaNode );
	}
}
