/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchIndexSchemaCollector<B extends AbstractIndexSchemaObjectNodeBuilder>
		implements IndexSchemaCollector {

	protected final B nodeBuilder;

	AbstractElasticsearchIndexSchemaCollector(B nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( ",nodeBuilder=" ).append( nodeBuilder )
				.append( "]" )
				.toString();
	}

	@Override
	public abstract ElasticsearchIndexSchemaElement withContext(IndexSchemaNestingContext context);

	@Override
	public ObjectFieldIndexSchemaCollector objectField(String relativeName, ObjectFieldStorage storage) {
		IndexSchemaObjectPropertyNodeBuilder nodeBuilder =
				new IndexSchemaObjectPropertyNodeBuilder( this.nodeBuilder.getAbsolutePath(), relativeName );
		nodeBuilder.setStorage( storage );
		this.nodeBuilder.putProperty( relativeName, nodeBuilder );
		return new ElasticsearchObjectFieldIndexSchemaCollectorImpl( nodeBuilder );
	}

}
