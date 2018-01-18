/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchIndexSchemaCollector<B extends AbstractIndexSchemaCompositeNodeBuilder<?>>
		implements IndexSchemaCollector {

	protected final JsonObjectAccessor accessor;
	protected final B nodeBuilder;

	AbstractElasticsearchIndexSchemaCollector(JsonObjectAccessor accessor, B nodeBuilder) {
		this.accessor = accessor;
		this.nodeBuilder = nodeBuilder;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "accessor=" ).append( accessor )
				.append( ",nodeBuilder=" ).append( nodeBuilder )
				.append( "]" )
				.toString();
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
	public IndexSchemaCollector objectField(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();
		IndexSchemaObjectPropertyNodeBuilder nestedNodeBuilder =
				new IndexSchemaObjectPropertyNodeBuilder( propertyAccessor );
		nodeBuilder.putProperty( relativeName, nestedNodeBuilder );
		return new ElasticsearchObjectFieldIndexSchemaCollectorImpl( propertyAccessor, nestedNodeBuilder );
	}

}
