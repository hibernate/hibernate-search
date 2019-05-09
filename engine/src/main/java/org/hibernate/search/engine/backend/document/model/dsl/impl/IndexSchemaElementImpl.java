/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeTerminalContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.StringHelper;

public class IndexSchemaElementImpl<B extends IndexSchemaObjectNodeBuilder> implements IndexSchemaElement {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeFactoryContext typeFactoryContext;
	final B objectNodeBuilder;
	private final IndexSchemaNestingContext nestingContext;
	private final boolean directChildrenAreMultiValuedByDefault;

	public IndexSchemaElementImpl(IndexFieldTypeFactoryContext typeFactoryContext,
			B objectNodeBuilder, IndexSchemaNestingContext nestingContext,
			boolean directChildrenAreMultiValuedByDefault) {
		this.typeFactoryContext = typeFactoryContext;
		this.objectNodeBuilder = objectNodeBuilder;
		this.nestingContext = nestingContext;
		this.directChildrenAreMultiValuedByDefault = directChildrenAreMultiValuedByDefault;
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
	public <F> IndexSchemaFieldContext<?, IndexFieldReference<F>> field(
			String relativeFieldName, IndexFieldType<F> type) {
		checkRelativeFieldName( relativeFieldName );
		IndexSchemaFieldContext<?, IndexFieldReference<F>> fieldContext = nestingContext.nest(
				relativeFieldName,
				// If the field is included
				prefixedName -> objectNodeBuilder.addField( prefixedName, type ),
				// If the field is filtered out
				prefixedName -> objectNodeBuilder.createExcludedField( prefixedName, type )
		);
		if ( directChildrenAreMultiValuedByDefault ) {
			fieldContext.multiValued();
		}
		return fieldContext;
	}

	@Override
	public <F> IndexSchemaFieldContext<?, IndexFieldReference<F>> field(String relativeFieldName,
			Function<? super IndexFieldTypeFactoryContext, ? extends IndexFieldTypeTerminalContext<F>> typeContributor) {
		return field( relativeFieldName, typeContributor.apply( typeFactoryContext ) );
	}

	@Override
	public IndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage) {
		checkRelativeFieldName( relativeFieldName );
		IndexSchemaObjectField fieldContext = nestingContext.nest(
				relativeFieldName,
				// If the field is included
				(prefixedName, filter) -> {
					IndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
							this.objectNodeBuilder.addObjectField( prefixedName, storage );
					return new IndexSchemaObjectFieldImpl( typeFactoryContext, objectFieldBuilder, filter, false );
				},
				// If the field is filtered out
				(prefixedName, filter) -> {
					IndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
							this.objectNodeBuilder.createExcludedObjectField( prefixedName, storage );
					return new IndexSchemaObjectFieldImpl( typeFactoryContext, objectFieldBuilder, filter, false );
				}
		);
		if ( directChildrenAreMultiValuedByDefault ) {
			fieldContext.multiValued();
		}
		return fieldContext;
	}

	private void checkRelativeFieldName(String relativeFieldName) {
		if ( StringHelper.isEmpty( relativeFieldName ) ) {
			throw log.relativeFieldNameCannotBeNullOrEmpty( relativeFieldName, objectNodeBuilder.getEventContext() );
		}
		if ( relativeFieldName.contains( "." ) ) {
			throw log.relativeFieldNameCannotContainDot( relativeFieldName, objectNodeBuilder.getEventContext() );
		}
	}

}
