/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaElement;

/**
 * @author Guillaume Smet
 */
abstract class AbstractLuceneIndexSchemaCollector<B extends AbstractIndexSchemaNodeBuilder>
		implements IndexSchemaCollector {

	protected final B nodeBuilder;

	AbstractLuceneIndexSchemaCollector(B nodeBuilder) {
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
	public abstract LuceneIndexSchemaElement withContext(IndexSchemaNestingContext context);

	@Override
	public ObjectFieldIndexSchemaCollector objectField(String relativeFieldName, ObjectFieldStorage storage) {
		IndexSchemaObjectPropertyNodeBuilder nodeBuilder =
				new IndexSchemaObjectPropertyNodeBuilder( this.nodeBuilder.getAbsolutePath(), relativeFieldName );
		nodeBuilder.setStorage( storage );
		this.nodeBuilder.putProperty( relativeFieldName, nodeBuilder );
		return new LuceneObjectFieldIndexSchemaCollectorImpl( nodeBuilder );
	}
}
