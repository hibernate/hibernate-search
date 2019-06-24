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

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectNodeBuilder, IndexSchemaBuildContext {
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
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(
			String relativeFieldName, IndexFieldType<F> indexFieldType) {
		LuceneIndexFieldType<F> luceneIndexFieldType = (LuceneIndexFieldType<F>) indexFieldType;
		LuceneIndexSchemaFieldNodeBuilder<F> childBuilder = new LuceneIndexSchemaFieldNodeBuilder<>(
				this, relativeFieldName, luceneIndexFieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> createExcludedField(
			String relativeFieldName, IndexFieldType<F> indexFieldType) {
		LuceneIndexFieldType<F> luceneIndexFieldType = (LuceneIndexFieldType<F>) indexFieldType;
		return new LuceneIndexSchemaFieldNodeBuilder<>(
				this, relativeFieldName, luceneIndexFieldType
		);
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		LuceneIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new LuceneIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		return new LuceneIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
	}

	public abstract LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final void contributeChildren(LuceneIndexSchemaObjectNode node, LuceneIndexSchemaNodeCollector collector) {
		for ( LuceneIndexSchemaNodeContributor contributor : content.values() ) {
			contributor.contribute( collector, node );
		}
	}

	private void putField(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = content.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, getEventContext() );
		}
	}
}
