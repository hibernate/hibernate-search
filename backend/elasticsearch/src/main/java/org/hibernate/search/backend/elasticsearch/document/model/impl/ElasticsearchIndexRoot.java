/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexRoot;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

public final class ElasticsearchIndexRoot
		extends AbstractIndexRoot<
				ElasticsearchIndexRoot,
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchIndexCompositeNodeType,
				ElasticsearchIndexField>
		implements ElasticsearchIndexCompositeNode {

	public ElasticsearchIndexRoot(ElasticsearchIndexCompositeNodeType type,
			Map<String, ElasticsearchIndexField> notYetInitializedStaticChildren) {
		super( type, notYetInitializedStaticChildren );
	}

	@Override
	protected ElasticsearchIndexRoot self() {
		return this;
	}

	@Override
	public ElasticsearchIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public ElasticsearchIndexValueField<?> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

}
