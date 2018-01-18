/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RoutingType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

public class ElasticsearchRootIndexSchemaCollectorImpl
		extends AbstractElasticsearchIndexSchemaCollector<IndexSchemaTypeNodeBuilder>
		implements ElasticsearchIndexSchemaNodeContributor<TypeMapping> {

	public ElasticsearchRootIndexSchemaCollectorImpl() {
		super( JsonAccessor.root(), new IndexSchemaTypeNodeBuilder( JsonAccessor.root() ) );
	}

	@Override
	public ElasticsearchIndexSchemaElement withContext(IndexSchemaNestingContext context) {
		/*
		 * Note: this ignores any previous nesting context, but that's alright since
		 * nesting context composition is handled in the engine.
		 */
		return new ElasticsearchIndexSchemaElementImpl( accessor, nodeBuilder, context );
	}

	@Override
	public void explicitRouting() {
		nodeBuilder.setRouting( RoutingType.REQUIRED );
	}

	@Override
	public TypeMapping contribute(ElasticsearchFieldModelCollector collector) {
		return nodeBuilder.contribute( collector );
	}
}
