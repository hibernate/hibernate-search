/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldFinalStep;
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
 * Test the behavior of implementations of {@link IndexSchemaElement} when defining fields.
 * <p>
 * This does not check the effects of the definitions on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
class IndexSchemaElementFieldIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private StubMappedIndex index;

	@Test
	void nullFieldName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field( null, this::irrelevantTypeContributor );
				} ),
				"Null field name on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field name 'null': field names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" )
							.field( null, this::irrelevantTypeContributor );
				} ),
				"Null field name on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field name 'null': field names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( null );
				} ),
				"Null object field name on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field name 'null': field names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).objectField( null );
				} ),
				"Null object field name on non-root"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid index field name 'null': field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field name 'null': field names cannot be null or empty" ) );
	}

	@Test
	void emptyFieldName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field( "", this::irrelevantTypeContributor );
				} ),
				"empty field name on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field name '': field names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).field( "", this::irrelevantTypeContributor );
				} ),
				"empty field name on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field name '': field names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "" );
				} ),
				"empty object field name on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Invalid index field name '': field names cannot be null or empty" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).objectField( "" );
				} ),
				"empty object field name on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure( "Invalid index field name '': field names cannot be null or empty" ) );
	}

	@Test
	void dotInFieldName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field( "foo.bar", this::irrelevantTypeContributor );
				} ),
				"field name containing a dot on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure(
								"Invalid index field name 'foo.bar': field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).field( "foo.bar", this::irrelevantTypeContributor );
				} ),
				"field name containing a dot on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Invalid index field name 'foo.bar': field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "foo.bar" );
				} ),
				"object field name containing a dot on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure(
								"Invalid index field name 'foo.bar': field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "nonRoot" ).objectField( "foo.bar" );
				} ),
				"object field name containing a dot on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Invalid index field name 'foo.bar': field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						) );
	}

	@Test
	void nameCollision_fields() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field( "field1", f -> f.asString() );
					root.field( "field1", this::irrelevantTypeContributor );
				} ),
				"Name collision between two fields on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Duplicate index field definition: 'field1'" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1", f -> f.asString() );
					objectField2.field( "field1", this::irrelevantTypeContributor );
				} ),
				"Name collision between two fields on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "object1.object2" )
						.failure( "Duplicate index field definition: 'field1'" ) );
	}

	@Test
	void nameCollision_objectFields() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "field1" );
					root.objectField( "field1" );
				} ),
				"Name collision between two object fields on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Duplicate index field definition: 'field1'" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.objectField( "field1" );
					objectField2.objectField( "field1" );
				} ),
				"Name collision between two object fields on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "object1.object2" )
						.failure( "Duplicate index field definition: 'field1'" ) );
	}

	@Test
	void nameCollision_fieldAndObjectField() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field( "field1", f -> f.asString() );
					root.objectField( "field1" );
				} ),
				"Name collision between two fields on root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexSchemaRootContext()
						.failure( "Duplicate index field definition: 'field1'" ) );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1", f -> f.asString() );
					objectField2.objectField( "field1" );
				} ),
				"Name collision between two fields (object and non-object) on non-root"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "object1.object2" )
						.failure( "Duplicate index field definition: 'field1'" ) );
	}

	@Test
	void missingToReferenceCall() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field( "myField", this::irrelevantTypeContributor );
				} ),
				"Missing toReference() call after field()"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "myField" )
						.failure( "Incomplete field definition" ) );
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.objectField( "myField" );
				} ),
				"Missing toReference() call after objectField()"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "myField" )
						.failure( "Incomplete field definition" ) );
	}

	@Test
	void multipleToReferenceCall() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaFieldFinalStep<?> context = root.field(
							"myField",
							this::irrelevantTypeContributor
					);
					context.toReference();
					context.toReference();
				} ),
				"Multiple toReference() calls after field()"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "myField" )
						.failure( "Multiple calls to toReference() for the same field definition" ) );
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					IndexSchemaObjectField context = root.objectField( "myField" );
					context.toReference();
					context.toReference();
				} ),
				"Multiple toReference() calls after objectField()"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.indexFieldContext( "myField" )
						.failure( "Multiple calls to toReference() for the same field definition" ) );
	}

	private IndexFieldTypeFinalStep<String> irrelevantTypeContributor(IndexFieldTypeFactory factoryContext) {
		return factoryContext.asString();
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		index = StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor );
		setupHelper.start().withIndex( index ).setup();
	}

}
