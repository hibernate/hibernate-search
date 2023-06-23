/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexCompositeNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexObjectFieldBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class IndexSchemaElementImpl<B extends IndexCompositeNodeBuilder> implements IndexSchemaElement {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexFieldTypeFactory typeFactory;
	final B objectNodeBuilder;
	private final TreeNestingContext nestingContext;
	private final boolean directChildrenAreMultiValuedByDefault;

	public IndexSchemaElementImpl(IndexFieldTypeFactory typeFactory,
			B objectNodeBuilder, TreeNestingContext nestingContext,
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
						(prefixedName, inclusion) -> objectNodeBuilder.addField( prefixedName, inclusion, type )
				);
		if ( directChildrenAreMultiValuedByDefault ) {
			fieldFinalStep.multiValued();
		}
		return fieldFinalStep;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep namedPredicate(String relativeNamedPredicateName,
			PredicateDefinition definition) {
		checkRelativeNamedPredicateName( relativeNamedPredicateName );
		return nestingContext.nestUnfiltered(
				(inclusion, prefix) ->
				// Ignore the prefix: it's not relevant here, and it's a deprecated feature anyway.
				objectNodeBuilder.addNamedPredicate( relativeNamedPredicateName, inclusion, definition )
		);
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> field(String relativeFieldName,
			Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<F>> typeContributor) {
		return field( relativeFieldName, typeContributor.apply( typeFactory ) );
	}

	@Override
	public IndexSchemaObjectField objectField(String relativeFieldName, ObjectStructure structure) {
		checkRelativeFieldName( relativeFieldName );
		IndexSchemaObjectField objectField = nestingContext.nest(
				relativeFieldName,
				(prefixedName, inclusion, nestedNestingContext) -> {
					IndexObjectFieldBuilder objectFieldBuilder =
							this.objectNodeBuilder.addObjectField( prefixedName, inclusion, structure );
					return new IndexSchemaObjectFieldImpl( typeFactory, objectFieldBuilder,
							nestedNestingContext, false );
				}
		);
		if ( directChildrenAreMultiValuedByDefault ) {
			objectField.multiValued();
		}
		return objectField;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> fieldTemplate(String templateName, IndexFieldType<?> type) {
		checkFieldTemplateName( templateName );
		IndexSchemaFieldTemplateOptionsStep<?> fieldTemplateFinalStep =
				// Filters are ignored for dynamic paths: as soon as the parent element is included,
				// all dynamic paths registered on that element are included.
				nestingContext.nestUnfiltered(
						(inclusion, prefix) -> objectNodeBuilder.addFieldTemplate(
								templateName, inclusion, type, prefix
						)
				);
		if ( directChildrenAreMultiValuedByDefault ) {
			fieldTemplateFinalStep.multiValued();
		}
		return fieldTemplateFinalStep;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> fieldTemplate(String templateName,
			Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<?>> typeContributor) {
		return fieldTemplate( templateName, typeContributor.apply( typeFactory ) );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> objectFieldTemplate(String templateName, ObjectStructure structure) {
		checkFieldTemplateName( templateName );
		IndexSchemaFieldTemplateOptionsStep<?> fieldTemplateFinalStep =
				// Filters are ignored for dynamic paths: as soon as the parent element is included,
				// all dynamic paths registered on that element are included.
				nestingContext.nestUnfiltered(
						(inclusion, prefix) -> objectNodeBuilder.addObjectFieldTemplate(
								templateName, structure, prefix, inclusion
						)
				);
		if ( directChildrenAreMultiValuedByDefault ) {
			fieldTemplateFinalStep.multiValued();
		}
		return fieldTemplateFinalStep;
	}

	private void checkRelativeFieldName(String relativeFieldName) {
		if ( StringHelper.isEmpty( relativeFieldName ) ) {
			throw log.relativeFieldNameCannotBeNullOrEmpty( relativeFieldName, objectNodeBuilder.eventContext() );
		}
		if ( relativeFieldName.contains( FieldPaths.PATH_SEPARATOR_STRING ) ) {
			throw log.relativeFieldNameCannotContainDot( relativeFieldName, objectNodeBuilder.eventContext() );
		}
	}

	private void checkFieldTemplateName(String templateName) {
		if ( StringHelper.isEmpty( templateName ) ) {
			throw log.fieldTemplateNameCannotBeNullOrEmpty( templateName, objectNodeBuilder.eventContext() );
		}
		// This is mostly to allow making template names absolute and unique by prepending them
		// with the path of the schema elements they were declared on.
		if ( templateName.contains( FieldPaths.PATH_SEPARATOR_STRING ) ) {
			throw log.fieldTemplateNameCannotContainDot( templateName, objectNodeBuilder.eventContext() );
		}
	}

	private void checkRelativeNamedPredicateName(String relativeFilterName) {
		if ( StringHelper.isEmpty( relativeFilterName ) ) {
			throw log.relativeNamedPredicateNameCannotBeNullOrEmpty( relativeFilterName, objectNodeBuilder.eventContext() );
		}
		if ( relativeFilterName.contains( "." ) ) {
			throw log.relativeNamedPredicateNameCannotContainDot( relativeFilterName, objectNodeBuilder.eventContext() );
		}
	}
}
