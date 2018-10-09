/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;


public class FieldSearchProjectionBuilderImpl<T> extends AbstractFieldSearchProjectionBuilderImpl<T, T>
		implements FieldSearchProjectionBuilder<T> {

	public FieldSearchProjectionBuilderImpl(ElasticsearchSearchTargetModel searchTargetModel,
			String absoluteFieldPath,
			Class<T> type) {
		super( searchTargetModel, absoluteFieldPath, type );
	}

	@Override
	protected ElasticsearchSearchProjection<T> createProjection(String absoluteFieldPath,
			ElasticsearchIndexSchemaFieldNode<T> schemaNode) {
		return new FieldSearchProjectionImpl<>( absoluteFieldPath, schemaNode.getConverter() );
	}
}
