/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Collections;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class ElasticsearchIndexSchemaObjectFieldTemplate
		extends AbstractElasticsearchIndexSchemaFieldTemplate<ElasticsearchIndexSchemaObjectFieldNode> {

	private final ObjectStructure structure;

	public ElasticsearchIndexSchemaObjectFieldTemplate(ElasticsearchIndexSchemaObjectNode declaringParent,
			SimpleGlobPattern absolutePathGlob, IndexFieldInclusion inclusion,
			boolean multiValued, ObjectStructure structure) {
		super( declaringParent, absolutePathGlob, inclusion, multiValued );
		this.structure = structure;
	}

	@Override
	protected ElasticsearchIndexSchemaObjectFieldNode createNode(ElasticsearchIndexSchemaObjectNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new ElasticsearchIndexSchemaObjectFieldNode(
				parent, relativePath, inclusion, structure, multiValued,
				Collections.emptyList()
		);
	}
}
