/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

public class ElasticsearchIndexValueFieldTemplate
		extends AbstractElasticsearchIndexFieldTemplate<ElasticsearchIndexValueFieldType<?>> {

	public ElasticsearchIndexValueFieldTemplate(ElasticsearchIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, TreeNodeInclusion inclusion,
			boolean multiValued, ElasticsearchIndexValueFieldType<?> type) {
		super( declaringParent, absolutePathGlob, type, inclusion, multiValued );
	}

	@Override
	protected ElasticsearchIndexField createNode(ElasticsearchIndexCompositeNode parent, String relativePath,
			ElasticsearchIndexValueFieldType<?> type, TreeNodeInclusion inclusion, boolean multiValued) {
		return new ElasticsearchIndexValueField<>( parent, relativePath, type, inclusion, multiValued );
	}
}
