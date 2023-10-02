/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test the behavior of implementations of {@link IndexSchemaElement} when defining field templates.
 * <p>
 * This does not check the effects of the definitions on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
class IndexSchemaElementFieldTemplateIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private StubMappedIndex index;

	@Test
	void nullName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.fieldTemplate( null, this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field template name 'null': field template names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" )
							.fieldTemplate( null, this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field template name 'null': field template names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectFieldTemplate( null );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field template name 'null': field template names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).objectFieldTemplate( null );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field template name 'null': field template names cannot be null or empty" ) );
	}

	@Test
	void emptyName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.fieldTemplate( "", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field template name '': field template names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).fieldTemplate( "", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field template name '': field template names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectFieldTemplate( "" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field template name '': field template names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).objectFieldTemplate( "" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field template name '': field template names cannot be null or empty" ) );
	}

	@Test
	void dotInName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.fieldTemplate( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure(
								"Invalid index field template name 'foo.bar': field template names cannot contain a dot ('.')."
						) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).fieldTemplate( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Invalid index field template name 'foo.bar': field template names cannot contain a dot ('.')."
						) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectFieldTemplate( "foo.bar" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure(
								"Invalid index field template name 'foo.bar': field template names cannot contain a dot ('.')."
						) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).objectFieldTemplate( "foo.bar" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Invalid index field template name 'foo.bar': field template names cannot contain a dot ('.')."
						) );
	}

	@Test
	void nameCollision_fieldTemplates() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.fieldTemplate( "field1", this::irrelevantTypeContributor );
					root.fieldTemplate( "field1", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Duplicate index field template definition: 'field1'" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.fieldTemplate( "field1", this::irrelevantTypeContributor );
					objectField2.fieldTemplate( "field1", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "object1.object2" )
						.failure( "Duplicate index field template definition: 'field1'" ) );
	}

	@Test
	void nameCollision_objectFieldTemplates() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectFieldTemplate( "field1" );
					root.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Duplicate index field template definition: 'field1'" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.objectFieldTemplate( "field1" );
					objectField2.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "object1.object2" )
						.failure( "Duplicate index field template definition: 'field1'" ) );
	}

	@Test
	void nameCollision_fieldTemplateAndObjectFieldTemplate() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.fieldTemplate( "field1", f -> f.asString() );
					root.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Duplicate index field template definition: 'field1'" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.fieldTemplate( "field1", f -> f.asString() );
					objectField2.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "object1.object2" )
						.failure( "Duplicate index field template definition: 'field1'" ) );
	}

	private IndexFieldTypeFinalStep<String> irrelevantTypeContributor(IndexFieldTypeFactory factoryContext) {
		return factoryContext.asString();
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		index = StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor ).typeName( "typeName" );
		setupHelper.start().withIndex( index ).setup();
	}

}
