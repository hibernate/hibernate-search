/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class ElasticsearchIndexSchemaFieldTemplate
		extends AbstractElasticsearchIndexSchemaFieldTemplate<ElasticsearchIndexSchemaFieldNode<?>> {

	private final ElasticsearchIndexFieldType<?> type;

	public ElasticsearchIndexSchemaFieldTemplate(ElasticsearchIndexSchemaObjectNode declaringParent,
			SimpleGlobPattern absolutePathGlob, IndexFieldInclusion inclusion,
			boolean multiValued, ElasticsearchIndexFieldType<?> type) {
		super( declaringParent, absolutePathGlob, inclusion, multiValued );
		this.type = type;
	}

	@Override
	protected ElasticsearchIndexSchemaFieldNode<?> createNode(ElasticsearchIndexSchemaObjectNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new ElasticsearchIndexSchemaFieldNode<>( parent, relativePath, inclusion, multiValued, type );
	}
}
