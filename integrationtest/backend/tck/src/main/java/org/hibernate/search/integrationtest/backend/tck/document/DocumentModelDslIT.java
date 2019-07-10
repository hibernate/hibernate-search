/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldFinalStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of the document model definition DSL.
 * <p>
 * This does not check the effects of the model definition on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
public class DocumentModelDslIT {

	private static final String TYPE_NAME = "TypeName";
	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final List<Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, ?>>> MAIN_TYPES =
			CollectionHelper.toImmutableList( CollectionHelper.asList(
					IndexFieldTypeFactory::asString,
					IndexFieldTypeFactory::asInteger,
					IndexFieldTypeFactory::asLocalDate,
					IndexFieldTypeFactory::asGeoPoint
			) );

	@Test
	public void nullFieldName() {
		SubTest.expectException(
				"Null field name on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( null, this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field name 'null' is invalid: field names cannot be null or empty" )
						.build() );

		SubTest.expectException(
				"Null field name on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" )
							.field( null, this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field name 'null' is invalid: field names cannot be null or empty" )
						.build() );

		SubTest.expectException(
				"Null object field name on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( null );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field name 'null' is invalid: field names cannot be null or empty" )
						.build() );

		SubTest.expectException(
				"Null object field name on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectField( null );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field name 'null' is invalid: field names cannot be null or empty" )
						.build() );
	}

	@Test
	public void emptyFieldName() {
		SubTest.expectException(
				"empty field name on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field name '' is invalid: field names cannot be null or empty" )
						.build() );

		SubTest.expectException(
				"empty field name on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).field( "", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field name '' is invalid: field names cannot be null or empty" )
						.build() );

		SubTest.expectException(
				"empty object field name on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field name '' is invalid: field names cannot be null or empty" )
						.build() );

		SubTest.expectException(
				"empty object field name on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectField( "" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field name '' is invalid: field names cannot be null or empty" )
						.build() );
	}

	@Test
	public void dotInFieldName() {
		SubTest.expectException(
				"field name containing a dot on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure(
								"Field name 'foo.bar' is invalid: field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						)
						.build() );

		SubTest.expectException(
				"field name containing a dot on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).field( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Field name 'foo.bar' is invalid: field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						)
						.build() );

		SubTest.expectException(
				"object field name containing a dot on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "foo.bar" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure(
								"Field name 'foo.bar' is invalid: field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						)
						.build() );

		SubTest.expectException(
				"object field name containing a dot on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectField( "foo.bar" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Field name 'foo.bar' is invalid: field names cannot contain a dot ('.').",
								" Remove the dot from your field name",
								"if you are declaring the field in a bridge and want a tree of fields,",
								" declare an object field using the objectField() method."
						)
						.build() );
	}

	@Test
	public void nameCollision_fields() {
		SubTest.expectException(
				"Name collision between two fields on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "field1", f -> f.asString() );
					root.field( "field1", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "schema node 'field1' was added twice" )
						.build() );

		SubTest.expectException(
				"Name collision between two fields on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1", f -> f.asString() );
					objectField2.field( "field1", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "object1.object2" )
						.failure( "schema node 'field1' was added twice" )
						.build() );
	}

	@Test
	public void nameCollision_objectFields() {
		SubTest.expectException(
				"Name collision between two object fields on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "field1" );
					root.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "schema node 'field1' was added twice" )
						.build() );

		SubTest.expectException(
				"Name collision between two object fields on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.objectField( "field1" );
					objectField2.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "object1.object2" )
						.failure( "schema node 'field1' was added twice" )
						.build() );
	}

	@Test
	public void nameCollision_fieldAndObjectField() {
		SubTest.expectException(
				"Name collision between two fields on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "field1", f -> f.asString() );
					root.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "schema node 'field1' was added twice" )
						.build() );

		SubTest.expectException(
				"Name collision between two fields (object and non-object) on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1", f -> f.asString() );
					objectField2.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "object1.object2" )
						.failure( "schema node 'field1' was added twice" )
						.build() );
	}

	@Test
	public void analyzerOnSortableField() {
		SubTest.expectException(
				"Setting an analyzer on sortable field",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field(
							"myField",
							f -> f.asString()
									.sortable( Sortable.YES )
									.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.toReference();
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								"Cannot apply an analyzer on a sortable field",
								"Use a normalizer instead",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'"
						)
						.build() );
	}

	@Test
	public void analyzerAndNormalizer() {
		SubTest.expectException(
				"Setting an analyzer and a normalizer on the same field",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field(
							"myField",
							f -> f.asString()
									.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
									.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
					)
							.toReference();
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								"Cannot apply both an analyzer and a normalizer",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'",
								"'" + DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name + "'"
						)
						.build() );
	}

	@Test
	public void missingGetReferenceCall() {
		for ( Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, ?>> typedContextFunction : MAIN_TYPES ) {
			SubTest.expectException(
					"Missing toReference() call after " + typedContextFunction,
					() -> setup( ctx -> {
						IndexSchemaElement root = ctx.getSchemaElement();
						root.field( "myField", typedContextFunction::apply );
					} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
							.typeContext( TYPE_NAME )
							.indexContext( INDEX_NAME )
							.indexFieldContext( "myField" )
							.failure( "Incomplete field definition" )
							.build() );
		}
		SubTest.expectException(
				"Missing toReference() call after objectField()",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "myField" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "myField" )
						.failure( "Incomplete field definition" )
						.build() );
	}

	@Test
	public void multipleGetReferenceCall() {
		for ( Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, ?>> typedContextFunction : MAIN_TYPES ) {
			SubTest.expectException(
					"Multiple toReference() calls after " + typedContextFunction,
					() -> setup( ctx -> {
						IndexSchemaElement root = ctx.getSchemaElement();
						IndexSchemaFieldFinalStep<?> context = root.field(
								"myField",
								typedContextFunction::apply
						);
						context.toReference();
						context.toReference();
					} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
							.typeContext( TYPE_NAME )
							.indexContext( INDEX_NAME )
							.indexFieldContext( "myField" )
							.failure( "Multiple calls to toReference() for the same field definition" )
							.build() );
		}
		SubTest.expectException(
				"Multiple toReference() calls after objectField()",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField context = root.objectField( "myField" );
					context.toReference();
					context.toReference();
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "myField" )
						.failure( "Multiple calls to toReference() for the same field definition" )
						.build() );
	}

	private IndexFieldTypeFinalStep<String> irrelevantTypeContributor(IndexFieldTypeFactory factoryContext) {
		return factoryContext.asString();
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						b -> b.mappedType( TYPE_NAME ),
						mappingContributor,
						ignored -> { }
				)
				.setup();
	}

}
