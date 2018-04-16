/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexObjectFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DynamicType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RoutingType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

class IndexSchemaTypeNodeBuilder extends AbstractIndexSchemaObjectNodeBuilder {

	private RoutingType routing = null;

	public void setRouting(RoutingType routing) {
		this.routing = routing;
	}

	@Override
	public String getAbsolutePath() {
		return null;
	}

	protected TypeMapping contribute(ElasticsearchIndexSchemaNodeCollector collector) {
		ElasticsearchIndexSchemaObjectNode node = ElasticsearchIndexSchemaObjectNode.root();

		accessor.initialize( new ElasticsearchIndexObjectFieldAccessor( JsonAccessor.root(), node ) );

		TypeMapping mapping = new TypeMapping();
		if ( routing != null ) {
			mapping.setRouting( routing );
		}

		// TODO allow to configure this, both at index level (configuration properties) and at field level (ElasticsearchExtension)
		mapping.setDynamic( DynamicType.STRICT );

		contributeChildren( mapping, node, collector );

		return mapping;
	}
}
