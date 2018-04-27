/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaObjectField;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSchemaElementImpl
		implements ElasticsearchIndexSchemaElement {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final AbstractIndexSchemaObjectNodeBuilder nodeBuilder;
	private final IndexSchemaNestingContext nestingContext;

	ElasticsearchIndexSchemaElementImpl(AbstractIndexSchemaObjectNodeBuilder nodeBuilder,
			IndexSchemaNestingContext nestingContext) {
		this.nodeBuilder = nodeBuilder;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absolutePath=" ).append( nodeBuilder.getAbsolutePath() )
				.append( ",nestingContext=" ).append( nestingContext )
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
					ElasticsearchIndexSchemaFieldContextImpl fieldContext =
							new ElasticsearchIndexSchemaFieldContextImpl( prefixedName );
					// Only take the contributor into account if the field is included
					nodeBuilder.putProperty( prefixedName, fieldContext );
					return fieldContext;
				},
				// If the field is filtered out
				ElasticsearchIndexSchemaFieldContextImpl::new
				);
	}

	@Override
	public ElasticsearchIndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage) {
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
					return new ElasticsearchIndexSchemaObjectFieldImpl( nodeBuilder, filter );
				},
				// If the field is filtered out
				(prefixedName, filter) -> {
					IndexSchemaObjectPropertyNodeBuilder nodeBuilder =
							new IndexSchemaObjectPropertyNodeBuilder( this.nodeBuilder.getAbsolutePath(), prefixedName );
					nodeBuilder.setStorage( storage );
					return new ElasticsearchIndexSchemaObjectFieldImpl( nodeBuilder, filter );
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
