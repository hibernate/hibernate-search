/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.dynamic;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for fields defined through field templates.
 * <p>
 * These tests are rather limited and focus on aspects that could potentially impact
 * the template matching during indexing and in search queries:
 * <ul>
 *     <li>Field structure: root, in an object, parent flattened/nested, ...</li>
 *     <li>Field cardinality: single-valued or multi-valued.</li>
 * </ul>
 * <p>
 * We do not test all features (sorts, aggregations, ...) because we expect the backend
 * to rely on the same generic code for all features.
 */
@RunWith(Parameterized.class)
public class FieldTemplateIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			parameters.add( new Object[] { fieldStructure } );
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";

	private static final String EMPTY = "empty";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final TestedFieldStructure fieldStructure;

	private SimpleMappedIndex<IndexBinding> index;

	public FieldTemplateIT(TestedFieldStructure fieldStructure) {
		this.fieldStructure = fieldStructure;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void simple() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			IndexSchemaFieldTemplateOptionsStep<?> step =
					root.fieldTemplate( "myTemplate", f -> f.asString() );
			if ( fieldStructure.isMultiValued() ) {
				step.multiValued();
			}
		};
		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_1, document ->
						initDocument( document, "foo",
								"matchedValue", "notMatchedValue1", "notMatchedValue2" ) )
				.add( DOCUMENT_2, document ->
						initDocument( document, "foo",
								"notMatchedValue1", "notMatchedValue1", "matchedValue" ) )
				.join();

		// Check that documents are indexed and the dynamic field can be searched
		assertThatQuery( query(
				f -> f.match().field( getFieldPath( "foo" ) ).matching( "matchedValue" ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		assertThatQuery( query(
				f -> f.match().field( getFieldPath( "foo" ) ).matching( "matchedValue" ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void matchingPathGlob() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			IndexSchemaFieldTemplateOptionsStep<?> step = root.fieldTemplate( "stringTemplate", f -> f.asString() )
					.matchingPathGlob( "*_str" );
			if ( fieldStructure.isMultiValued() ) {
				step.multiValued();
			}
			step = root.fieldTemplate( "intTemplate", f -> f.asInteger() )
					.matchingPathGlob( "*_int" );
			if ( fieldStructure.isMultiValued() ) {
				step.multiValued();
			}
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_1, document ->
						initDocument( document, "foo_str",
								"matchedValue", "notMatchedValue1", "notMatchedValue2" ) )
				.add( DOCUMENT_2, document ->
						initDocument( document, "foo_int",
								42, 52, 56 ) )
				.join();

		// Check that documents are indexed and the dynamic fields can be searched
		assertThatQuery( query(
				f -> f.match().field( getFieldPath( "foo_str" ) ).matching( "matchedValue" ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( query(
				f -> f.match().field( getFieldPath( "foo_int" ) ).matching( 42 ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		assertThatQuery( query(
				f -> f.match().field( getFieldPath( "foo_str" ) ).matching( "matchedValue" ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( query(
				f -> f.match().field( getFieldPath( "foo_int" ) ).matching( 42 ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void matchingPathGlob_precedence_firstDeclared() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			IndexSchemaFieldTemplateOptionsStep<?> step = root.fieldTemplate( "stringTemplate", f -> f.asString() )
					.matchingPathGlob( "*_str_int" );
			if ( fieldStructure.isMultiValued() ) {
				step.multiValued();
			}
			step = root.fieldTemplate( "intTemplate", f -> f.asInteger() )
					.matchingPathGlob( "*_int" );
			if ( fieldStructure.isMultiValued() ) {
				step.multiValued();
			}
			step = root.fieldTemplate( "ignoredTemplate", f -> f.asLong() )
					.matchingPathGlob( "*_int" );
			if ( fieldStructure.isMultiValued() ) {
				step.multiValued();
			}
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_1, document ->
						initDocument( document, "foo_str_int",
								"42", "notMatchedValue1", "notMatchedValue2" ) )
				.add( DOCUMENT_2, document ->
						initDocument( document, "foo_int",
								42, 52, 56 ) )
				.join();

		// Check that dynamic fields have the correct type
		assertThatQuery( query(
				f -> f.range().field( getFieldPath( "foo_str_int" ) )
						.between( "3000", "5000" ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( query(
				f -> f.range().field( getFieldPath( "foo_int" ) )
						.between( 41, 43 ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		assertThatQuery( query(
				f -> f.range().field( getFieldPath( "foo_str_int" ) )
						.between( "3000", "5000" ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( query(
				f -> f.range().field( getFieldPath( "foo_int" ) )
						.between( 41, 43 ) )
		)
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	private SearchQuery<DocumentReference> query(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return index.createScope().query()
				.where( f -> {
					if ( fieldStructure.isInNested() ) {
						return f.nested().objectField( getParentFieldPath() )
								.nest( f.bool()
										.must( predicateContributor )
										.must( f.match().field( getFieldPath( "discriminator" ) )
												.matching( "included" ) )
								);
					}
					else {
						return predicateContributor.apply( f );
					}
				} )
				.toQuery();
	}

	private String getFieldPath(String relativeFieldName) {
		String parentFieldPath = getParentFieldPath();
		if ( parentFieldPath.isEmpty() ) {
			return relativeFieldName;
		}
		else {
			return parentFieldPath + "." + relativeFieldName;
		}
	}

	private String getParentFieldPath() {
		IndexBinding binding = index.binding();
		switch ( fieldStructure.location ) {
			case ROOT:
				return "";
			case IN_FLATTENED:
				return binding.flattenedObject.relativeFieldName;
			case IN_NESTED:
				return binding.nestedObject.relativeFieldName;
			case IN_NESTED_TWICE:
				return binding.nestedObject.relativeFieldName
						+ "." + binding.nestedObject.nestedObject.relativeFieldName;
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure.location );
		}
	}

	private SearchIntegration setup(StubMappingSchemaManagementStrategy schemaManagementStrategy,
			Consumer<IndexSchemaElement> templatesBinder) {
		this.index = SimpleMappedIndex.of( root -> new IndexBinding( root, templatesBinder ) );

		return setupHelper.start().withIndex( index )
				.withSchemaManagement( schemaManagementStrategy )
				.setup();
	}

	private <T> void initDocument(DocumentElement document, String fieldName,
			T value1, T value2, T excludedValue) {
		IndexBinding binding = index.binding();
		switch ( fieldStructure.location ) {
			case ROOT:
				document.addValue( fieldName, value1 );
				if ( fieldStructure.isMultiValued() ) {
					document.addValue( fieldName, value2 );
				}
				break;
			case IN_FLATTENED:
				DocumentElement flattenedObject = document.addObject( binding.flattenedObject.self );
				flattenedObject.addValue( fieldName, value1 );
				if ( fieldStructure.isMultiValued() ) {
					flattenedObject.addValue( fieldName, value2 );
				}
				break;
			case IN_NESTED:
				DocumentElement nestedObject0 = document.addObject( binding.nestedObject.self );
				nestedObject0.addValue( binding.nestedObject.discriminator, "included" );
				nestedObject0.addValue( fieldName, value1 );
				DocumentElement nestedObject1 = document.addObject( binding.nestedObject.self );
				nestedObject1.addValue( binding.nestedObject.discriminator, "excluded" );
				nestedObject1.addValue( fieldName, excludedValue );
				DocumentElement nestedObject2 = document.addObject( binding.nestedObject.self );
				nestedObject2.addValue( binding.nestedObject.discriminator, "included" );
				nestedObject2.addValue( fieldName, value2 );
				break;
			case IN_NESTED_TWICE:
				DocumentElement firstLevelNestedObject0 = document.addObject( binding.nestedObject.self );
				DocumentElement firstLevelNestedObject1 = document.addObject( binding.nestedObject.self );
				DocumentElement nestedNestedObject0 = firstLevelNestedObject0.addObject( binding.nestedObject.nestedObject.self );
				nestedNestedObject0.addValue( binding.nestedObject.nestedObject.discriminator, "included" );
				nestedNestedObject0.addValue( fieldName, value1 );
				DocumentElement nestedNestedObject1 = firstLevelNestedObject0.addObject( binding.nestedObject.nestedObject.self );
				nestedNestedObject1.addValue( binding.nestedObject.nestedObject.discriminator, "excluded" );
				nestedNestedObject1.addValue( fieldName, excludedValue );
				DocumentElement nestedNestedObject2 = firstLevelNestedObject1.addObject( binding.nestedObject.nestedObject.self );
				nestedNestedObject2.addValue( binding.nestedObject.nestedObject.discriminator, "included" );
				nestedNestedObject2.addValue( fieldName, value2 );
				break;
		}
	}

	private class IndexBinding {
		final FlattenedObjectBinding flattenedObject;
		final NestedObjectBinding nestedObject;

		IndexBinding(IndexSchemaElement root, Consumer<IndexSchemaElement> templatesBinder) {
			if ( fieldStructure.location.equals( IndexFieldLocation.ROOT ) ) {
				templatesBinder.accept( root );
			}

			flattenedObject = new FlattenedObjectBinding( root, "flattenedObject", templatesBinder );
			nestedObject = new NestedObjectBinding( root, "nestedObject", templatesBinder );
		}
	}

	private class FlattenedObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		private FlattenedObjectBinding(IndexSchemaElement parent, String relativeFieldName,
				Consumer<IndexSchemaElement> templatesBinder) {
			this.relativeFieldName = relativeFieldName;

			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.FLATTENED );
			self = objectField.toReference();

			if ( fieldStructure.location.equals( IndexFieldLocation.IN_FLATTENED ) ) {
				templatesBinder.accept( objectField );
			}
		}
	}

	private class NestedObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;

		final SecondLevelNestedObjectBinding nestedObject;

		private NestedObjectBinding(IndexSchemaElement parent, String relativeFieldName,
				Consumer<IndexSchemaElement> templatesBinder) {
			this.relativeFieldName = relativeFieldName;

			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.NESTED );
			objectField.multiValued();
			self = objectField.toReference();

			if ( fieldStructure.location.equals( IndexFieldLocation.IN_NESTED ) ) {
				templatesBinder.accept( objectField );
			}

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();

			nestedObject = new SecondLevelNestedObjectBinding( objectField, "nestedObject",
					templatesBinder );
		}
	}

	private class SecondLevelNestedObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;

		private SecondLevelNestedObjectBinding(IndexSchemaElement parent, String relativeFieldName,
				Consumer<IndexSchemaElement> templatesBinder) {
			this.relativeFieldName = relativeFieldName;

			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.NESTED );
			objectField.multiValued();
			self = objectField.toReference();

			if ( fieldStructure.location.equals( IndexFieldLocation.IN_NESTED_TWICE ) ) {
				templatesBinder.accept( objectField );
			}

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();
		}
	}
}
