/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaElement;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaObjectField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class LuceneIndexSchemaElementImpl implements LuceneIndexSchemaElement {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final AbstractIndexSchemaNodeBuilder nodeBuilder;

	private final IndexSchemaNestingContext nestingContext;

	LuceneIndexSchemaElementImpl(AbstractIndexSchemaNodeBuilder nodeBuilder, IndexSchemaNestingContext nestingContext) {
		this.nodeBuilder = nodeBuilder;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "nestingContext=" ).append( nestingContext )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexSchemaFieldContext field(String relativeFieldName) {
		checkRelativeFieldName( relativeFieldName );
		return nestingContext.nest(
				relativeFieldName,
				// If the field is included
				prefixedName -> {
					LuceneIndexSchemaFieldContextImpl fieldContext = new LuceneIndexSchemaFieldContextImpl( prefixedName );
					nodeBuilder.putProperty( prefixedName, fieldContext );
					return fieldContext;
				},
				// If the field is filtered out
				LuceneIndexSchemaFieldContextImpl::new
		);
	}

	@Override
	public LuceneIndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage) {
		checkRelativeFieldName( relativeFieldName );
		return nestingContext.nest(
				relativeFieldName,
				// If the field is included
				(prefixedName, filter) -> {
					IndexSchemaObjectPropertyNodeBuilder nodeBuilder =
							new IndexSchemaObjectPropertyNodeBuilder( this.nodeBuilder.getAbsolutePath(), prefixedName );
					nodeBuilder.setStorage( storage );
					// Only take the contributor into account if the field is included
					this.nodeBuilder.putProperty( prefixedName, nodeBuilder );
					return new LuceneIndexSchemaObjectFieldImpl( nodeBuilder, filter );
				},
				// If the field is filtered out
				(prefixedName, filter) -> {
					IndexSchemaObjectPropertyNodeBuilder nodeBuilder =
							new IndexSchemaObjectPropertyNodeBuilder( this.nodeBuilder.getAbsolutePath(), prefixedName );
					nodeBuilder.setStorage( storage );
					return new LuceneIndexSchemaObjectFieldImpl( nodeBuilder, filter );
				} );
	}

	private static void checkRelativeFieldName(String relativeFieldName) {
		if ( StringHelper.isEmpty( relativeFieldName ) ) {
			throw log.relativeFieldNameCannotBeNullOrEmpty( relativeFieldName );
		}
		if ( relativeFieldName.contains( "." ) ) {
			throw log.relativeFieldNameCannotContainDot( relativeFieldName );
		}
	}
}
