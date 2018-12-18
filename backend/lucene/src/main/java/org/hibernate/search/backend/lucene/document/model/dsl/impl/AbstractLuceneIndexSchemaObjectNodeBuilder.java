/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIndexFieldTypeFactoryContextImpl;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractLuceneIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder, LuceneIndexSchemaContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, LuceneIndexSchemaNodeContributor> content = new LinkedHashMap<>();

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absolutePath=" ).append( getAbsolutePath() )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexFieldTypeFactoryContext addField(String relativeFieldName) {
		LuceneIndexFieldTypeFactoryContextImpl fieldContext =
				new LuceneIndexFieldTypeFactoryContextImpl( getRoot(), getAbsolutePath(), relativeFieldName );
		putProperty( relativeFieldName, fieldContext );
		return fieldContext;
	}

	@Override
	public IndexFieldTypeFactoryContext createExcludedField(String relativeFieldName) {
		return new LuceneIndexFieldTypeFactoryContextImpl( getRoot(), getAbsolutePath(), relativeFieldName );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		LuceneIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new LuceneIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
		putProperty( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		return new LuceneIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
	}

	@Override
	public abstract LuceneIndexSchemaRootNodeBuilder getRoot();

	abstract String getAbsolutePath();

	final void contributeChildren(LuceneIndexSchemaObjectNode node, LuceneIndexSchemaNodeCollector collector) {
		for ( LuceneIndexSchemaNodeContributor contributor : content.values() ) {
			contributor.contribute( collector, node );
		}
	}

	private void putProperty(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = content.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, getEventContext() );
		}
	}
}
