/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchRootIndexSchemaContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RoutingType;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;

public class ElasticsearchRootIndexSchemaCollectorImpl
		extends AbstractElasticsearchIndexSchemaCollector<IndexSchemaRootTypeNodeBuilder> implements
		ElasticsearchRootIndexSchemaContributor {

	public ElasticsearchRootIndexSchemaCollectorImpl(MultiTenancyStrategy multiTenancyStrategy) {
		super( new IndexSchemaRootTypeNodeBuilder( multiTenancyStrategy ) );
	}

	@Override
	public ElasticsearchIndexSchemaElement withContext(IndexSchemaNestingContext context) {
		/*
		 * Note: this ignores any previous nesting context, but that's alright since
		 * nesting context composition is handled in the engine.
		 */
		return new ElasticsearchIndexSchemaElementImpl( nodeBuilder, context );
	}

	@Override
	public void explicitRouting() {
		nodeBuilder.setRouting( RoutingType.REQUIRED );
	}

	@Override
	public RootTypeMapping contribute(ElasticsearchIndexSchemaNodeCollector collector) {
		return nodeBuilder.contribute( collector );
	}
}
