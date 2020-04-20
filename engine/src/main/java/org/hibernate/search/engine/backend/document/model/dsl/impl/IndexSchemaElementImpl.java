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
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.StringHelper;

public class IndexSchemaElementImpl<B extends IndexSchemaObjectNodeBuilder> implements IndexSchemaElement {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeFactory typeFactory;
	final B objectNodeBuilder;
	private final IndexSchemaNestingContext nestingContext;
	private final boolean directChildrenAreMultiValuedByDefault;

	public IndexSchemaElementImpl(IndexFieldTypeFactory typeFactory,
			B objectNodeBuilder, IndexSchemaNestingContext nestingContext,
			boolean directChildrenAreMultiValuedByDefault) {
		this.typeFactory = typeFactory;
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
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> field(
			String relativeFieldName, IndexFieldType<F> type) {
		checkRelativeFieldName( relativeFieldName );
		IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> fieldFinalStep =
				// Explicit type parameter needed in order for JDT to compile correctly (probably a bug)
				nestingContext.<IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>>>nest(
						relativeFieldName,
						// If the field is included
						prefixedName -> objectNodeBuilder.addField( prefixedName, type ),
						// If the field is filtered out
						prefixedName -> objectNodeBuilder.createExcludedField( prefixedName, type )
				);
		if ( directChildrenAreMultiValuedByDefault ) {
			fieldFinalStep.multiValued();
		}
		return fieldFinalStep;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> field(String relativeFieldName,
			Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<F>> typeContributor) {
		return field( relativeFieldName, typeContributor.apply( typeFactory ) );
	}

	@Override
	public IndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage) {
		checkRelativeFieldName( relativeFieldName );
		IndexSchemaObjectField objectField = nestingContext.nest(
				relativeFieldName,
				// If the field is included
				(prefixedName, filter) -> {
					IndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
							this.objectNodeBuilder.addObjectField( prefixedName, storage );
					return new IndexSchemaObjectFieldImpl( typeFactory, objectFieldBuilder, filter, false );
				},
				// If the field is filtered out
				(prefixedName, filter) -> {
					IndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
							this.objectNodeBuilder.createExcludedObjectField( prefixedName, storage );
					return new IndexSchemaObjectFieldImpl( typeFactory, objectFieldBuilder, filter, false );
				}
		);
		if ( directChildrenAreMultiValuedByDefault ) {
			objectField.multiValued();
		}
		return objectField;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> fieldTemplate(String templateName, IndexFieldType<?> type) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> fieldTemplate(String templateName,
			Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<?>> typeContributor) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> objectFieldTemplate(String templateName, ObjectFieldStorage storage) {
		throw new UnsupportedOperationException( "Not implemented yet" );
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
