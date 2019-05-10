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
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeTerminalContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.reporting.spi.EventContexts;
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

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final List<Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, ?>>> MAIN_TYPES =
			CollectionHelper.toImmutableList( CollectionHelper.asList(
					IndexFieldTypeFactoryContext::asString,
					IndexFieldTypeFactoryContext::asInteger,
					IndexFieldTypeFactoryContext::asLocalDate,
					IndexFieldTypeFactoryContext::asGeoPoint
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
				.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

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
				.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "nonRoot" )
				) );

		SubTest.expectException(
				"Null object field name on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( null );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

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
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "nonRoot" )
				) );
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
				.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

		SubTest.expectException(
				"empty field name on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).field( "", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "nonRoot" )
				) );

		SubTest.expectException(
				"empty object field name on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

		SubTest.expectException(
				"empty object field name on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectField( "" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "nonRoot" )
				) );
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
				.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
				.hasMessageContaining( " Remove the dot from your field name" )
				.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
						+ " declare an object field using the objectField() method." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

		SubTest.expectException(
				"field name containing a dot on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).field( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
				.hasMessageContaining( " Remove the dot from your field name" )
				.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
						+ " declare an object field using the objectField() method." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "nonRoot" )
				) );

		SubTest.expectException(
				"object field name containing a dot on root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "foo.bar" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
				.hasMessageContaining( " Remove the dot from your field name" )
				.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
						+ " declare an object field using the objectField() method." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

		SubTest.expectException(
				"object field name containing a dot on non-root",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectField( "foo.bar" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
				.hasMessageContaining( " Remove the dot from your field name" )
				.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
						+ " declare an object field using the objectField() method." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "nonRoot" )
				) );
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
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

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
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "object1.object2" )
				) );
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
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

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
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "object1.object2" )
				) );
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
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.indexSchemaRoot()
				) );

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
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "object1.object2" )
				) );
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
				.hasMessageContaining( "Cannot apply an analyzer on a sortable field" )
				.hasMessageContaining( "Use a normalizer instead" )
				.hasMessageContaining( "'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME )
				) );
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
				.hasMessageContaining( "Cannot apply both an analyzer and a normalizer" )
				.hasMessageContaining( "'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'" )
				.hasMessageContaining( "'" + DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME )
				) );
	}

	@Test
	public void missingGetReferenceCall() {
		for ( Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, ?>> typedContextFunction : MAIN_TYPES ) {
			SubTest.expectException(
					"Missing toReference() call after " + typedContextFunction,
					() -> setup( ctx -> {
						IndexSchemaElement root = ctx.getSchemaElement();
						root.field( "myField", typedContextFunction::apply );
					} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Incomplete field definition" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexName( INDEX_NAME ),
							EventContexts.fromIndexFieldAbsolutePath( "myField" )
					) );
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
				.hasMessageContaining( "Incomplete field definition" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "myField" )
				) );
	}

	@Test
	public void multipleGetReferenceCall() {
		for ( Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, ?>> typedContextFunction : MAIN_TYPES ) {
			SubTest.expectException(
					"Multiple toReference() calls after " + typedContextFunction,
					() -> setup( ctx -> {
						IndexSchemaElement root = ctx.getSchemaElement();
						IndexSchemaFieldTerminalContext<?> context = root.field(
								"myField",
								typedContextFunction::apply
						);
						context.toReference();
						context.toReference();
					} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple calls to toReference() for the same field definition" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexName( INDEX_NAME ),
							EventContexts.fromIndexFieldAbsolutePath( "myField" )
					) );
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
				.hasMessageContaining( "Multiple calls to toReference() for the same field definition" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexName( INDEX_NAME ),
						EventContexts.fromIndexFieldAbsolutePath( "myField" )
				) );
	}

	private IndexFieldTypeTerminalContext<String> irrelevantTypeContributor(IndexFieldTypeFactoryContext factoryContext) {
		return factoryContext.asString();
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						mappingContributor
				)
				.setup();
	}

}
