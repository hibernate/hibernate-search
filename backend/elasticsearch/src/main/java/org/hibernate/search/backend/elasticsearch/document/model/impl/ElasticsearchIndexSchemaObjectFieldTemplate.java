/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class ElasticsearchIndexSchemaObjectFieldTemplate
		extends AbstractElasticsearchIndexSchemaFieldTemplate<ElasticsearchIndexSchemaObjectFieldNode> {

	private final ObjectFieldStorage storage;

	public ElasticsearchIndexSchemaObjectFieldTemplate(ElasticsearchIndexSchemaObjectNode declaringParent,
			SimpleGlobPattern absolutePathGlob, IndexFieldInclusion inclusion,
			boolean multiValued, ObjectFieldStorage storage) {
		super( declaringParent, absolutePathGlob, inclusion, multiValued );
		this.storage = storage;
	}

	@Override
	protected ElasticsearchIndexSchemaObjectFieldNode createNode(ElasticsearchIndexSchemaObjectNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new ElasticsearchIndexSchemaObjectFieldNode( parent, relativePath, inclusion, storage, multiValued );
	}
}
