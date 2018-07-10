/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

public class IndexSchemaElementImpl<B extends IndexSchemaObjectNodeBuilder> implements IndexSchemaElement {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final B objectNodeBuilder;
	private final IndexSchemaNestingContext nestingContext;

	public IndexSchemaElementImpl(B objectNodeBuilder,
			IndexSchemaNestingContext nestingContext) {
		this.objectNodeBuilder = objectNodeBuilder;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "objectNodeBuilder=" ).append( objectNodeBuilder )
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
				objectNodeBuilder::addField,
				// If the field is filtered out
				objectNodeBuilder::createExcludedField
		);
	}

	@Override
	public IndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage) {
		checkRelativeFieldName( relativeFieldName );
		return nestingContext.nest(
				relativeFieldName,
				// If the field is included
				(prefixedName, filter) -> {
					IndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
							this.objectNodeBuilder.addObjectField( prefixedName, storage );
					return new IndexSchemaObjectFieldImpl( objectFieldBuilder, filter );
				},
				// If the field is filtered out
				(prefixedName, filter) -> {
					IndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
							this.objectNodeBuilder.createExcludedObjectField( prefixedName, storage );
					return new IndexSchemaObjectFieldImpl( objectFieldBuilder, filter );
				}
		);
	}

	private void checkRelativeFieldName(String relativeFieldName) {
		if ( StringHelper.isEmpty( relativeFieldName ) ) {
			throw log.relativeFieldNameCannotBeNullOrEmpty( relativeFieldName, objectNodeBuilder.getFailureContext() );
		}
		if ( relativeFieldName.contains( "." ) ) {
			throw log.relativeFieldNameCannotContainDot( relativeFieldName, objectNodeBuilder.getFailureContext() );
		}
	}

}
