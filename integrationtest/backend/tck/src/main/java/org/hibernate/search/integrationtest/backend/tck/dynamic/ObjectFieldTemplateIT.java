/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.dynamic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for object fields defined through field templates.
 */
public class ObjectFieldTemplateIT {

	private static final String DOCUMENT_MATCHING_FOR_NESTED = "matchingNested";
	private static final String DOCUMENT_MATCHING_FOR_ALL = "matchingForAll";

	private static final String EMPTY = "empty";

	private static final String VALUE_FIELD_PATH_GLOB = "*_str";
	private static final String FIRSTNAME_FIELD = "firstName_str";
	private static final String LASTNAME_FIELD = "lastName_str";

	private static final String FIRSTNAME_1 = "daniel";
	private static final String LASTNAME_1 = "abraham";
	private static final String FIRSTNAME_2 = "ty";
	private static final String LASTNAME_2 = "frank";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private SimpleMappedIndex<IndexBinding> index;

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void simple() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.multiValued();
		};

		StubMapping mapping =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_MATCHING_FOR_NESTED, document -> {
					DocumentElement nestedObject = document.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					nestedObject = document.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
				} )
				.add( DOCUMENT_MATCHING_FOR_ALL, document -> {
					DocumentElement nestedObject = document.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					nestedObject = document.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.join();

		// Check that documents are indexed and the dynamic fields can be searched
		checkNested( "foo" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		checkNested( "foo" );
	}

	/**
	 * Templates defined on a given object should also be taken into account in their children.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void inherited() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.multiValued();
		};

		StubMapping mapping =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_MATCHING_FOR_NESTED, document -> {
					DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

					DocumentElement nestedObject = staticObject.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					nestedObject = staticObject.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
				} )
				.add( DOCUMENT_MATCHING_FOR_ALL, document -> {
					DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

					DocumentElement nestedObject = staticObject.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					nestedObject = staticObject.addObject( "foo" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.join();

		// Check that documents are indexed and the dynamic fields can be searched
		checkNested( "staticObject.foo" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		checkNested( "staticObject.foo" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void matchingPathGlob() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_nested" )
					.multiValued();
			root.objectFieldTemplate( "flattenedTemplate", ObjectStructure.FLATTENED )
					.matchingPathGlob( "*_flattened" )
					.multiValued();
		};

		StubMapping mapping =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_MATCHING_FOR_NESTED, document -> {
					DocumentElement nestedObject = document.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					nestedObject = document.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );

					DocumentElement flattenedObject = document.addObject( "bar_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					flattenedObject = document.addObject( "bar_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
				} )
				.add( DOCUMENT_MATCHING_FOR_ALL, document -> {
					DocumentElement nestedObject = document.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					nestedObject = document.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );

					DocumentElement flattenedObject = document.addObject( "bar_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					flattenedObject = document.addObject( "bar_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.join();

		// Check that documents are indexed and the dynamic fields can be searched
		checkNested( "foo_nested" );
		checkFlattened( "bar_flattened" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		checkNested( "foo_nested" );
		checkFlattened( "bar_flattened" );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3273", "HSEARCH-4048" })
	public void matchingPathGlob_precedence_firstDeclared() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_nested_object" )
					.multiValued();
			root.objectFieldTemplate( "flattenedTemplate", ObjectStructure.FLATTENED )
					.matchingPathGlob( "*_object" )
					.multiValued();
			root.objectFieldTemplate( "ignoredTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_object" );
			root.fieldTemplate( "ignoredFieldTemplate", f -> f.asString() )
					.matchingPathGlob( "*_object" );
		};

		StubMapping mapping =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_MATCHING_FOR_NESTED, document -> {
					DocumentElement nestedObject = document.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					nestedObject = document.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );

					DocumentElement flattenedObject = document.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					flattenedObject = document.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
				} )
				.add( DOCUMENT_MATCHING_FOR_ALL, document -> {
					DocumentElement nestedObject = document.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					nestedObject = document.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );

					DocumentElement flattenedObject = document.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					flattenedObject = document.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.join();

		// Check that dynamic fields have the correct structure
		checkNested( "foo_nested_object" );
		checkFlattened( "flattened_object" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		checkNested( "foo_nested_object" );
		checkFlattened( "flattened_object" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void matchingPathGlob_precedence_deepestDeclared() {
		Consumer<IndexSchemaElement> rootTemplatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "flattenedTemplate", ObjectStructure.FLATTENED )
					.matchingPathGlob( "*_object" )
					.multiValued();
		};
		Consumer<IndexSchemaElement> staticObjectTemplatesBinder = staticObject -> {
			staticObject.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_nested_object" )
					.multiValued();
		};

		StubMapping mapping = setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_MATCHING_FOR_NESTED, document -> {
					DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

					DocumentElement nestedObject = staticObject.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					nestedObject = staticObject.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );

					DocumentElement flattenedObject = staticObject.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					flattenedObject = staticObject.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
				} )
				.add( DOCUMENT_MATCHING_FOR_ALL, document -> {
					DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

					DocumentElement nestedObject = staticObject.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					nestedObject = staticObject.addObject( "foo_nested_object" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );

					DocumentElement flattenedObject = staticObject.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
					flattenedObject = staticObject.addObject( "flattened_object" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.join();

		// Check that dynamic fields have the correct structure
		checkNested( "staticObject.foo_nested_object" );
		checkFlattened( "staticObject.flattened_object" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		checkNested( "staticObject.foo_nested_object" );
		checkFlattened( "staticObject.flattened_object" );
	}

	/**
	 * The {@code exists} predicate should detect static object fields even if all their sub-fields are dynamic.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3273", "HSEARCH-3905" })
	public void exists_staticObjectField() {
		Consumer<IndexSchemaElement> rootTemplatesBinder = root -> { };
		Consumer<IndexSchemaElement> staticObjectTemplatesBinder = staticObject -> {
			staticObject.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			staticObject.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_nested" )
					.multiValued();
			staticObject.objectFieldTemplate( "flattenedTemplate", ObjectStructure.FLATTENED )
					.matchingPathGlob( "*_flattened" )
					.multiValued();
		};

		StubMapping mapping = setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		String documentWhereObjectFieldExistsId = "existing";
		String documentWhereObjectFieldDoesNotExistId = "not-existing";

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( documentWhereObjectFieldExistsId, document -> {
					DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

					DocumentElement nestedObject = staticObject.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					DocumentElement flattenedObject = staticObject.addObject( "foo_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.add( documentWhereObjectFieldDoesNotExistId, document -> {
					DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

					DocumentElement nestedObject = staticObject.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, null );
					nestedObject.addValue( LASTNAME_FIELD, null );
					DocumentElement flattenedObject = staticObject.addObject( "foo_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, null );
					flattenedObject.addValue( LASTNAME_FIELD, null );
				} )
				.join();

		// Check that documents are indexed and the dynamic object field can be detected through an exists() predicate
		assertThatQuery( query( f -> f.exists().field( "staticObject" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), documentWhereObjectFieldExistsId );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		assertThatQuery( query( f -> f.exists().field( "staticObject" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), documentWhereObjectFieldExistsId );
	}

	/**
	 * The {@code exists} predicate should detect dynamic object fields even if all their sub-fields are dynamic.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3273", "HSEARCH-3905" })
	public void exists_dynamicObjectField() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_nested" )
					.multiValued();
			root.objectFieldTemplate( "flattenedTemplate", ObjectStructure.FLATTENED )
					.matchingPathGlob( "*_flattened" )
					.multiValued();
		};

		StubMapping mapping =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		String documentWhereObjectFieldExistsId = "existing";
		String documentWhereObjectFieldDoesNotExistId = "not-existing";

		// Index a few documents
		index.bulkIndexer()
				.add( EMPTY, document -> { } )
				.add( documentWhereObjectFieldExistsId, document -> {
					DocumentElement nestedObject = document.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
					DocumentElement flattenedObject = document.addObject( "foo_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
					flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
				} )
				.add( documentWhereObjectFieldDoesNotExistId, document -> {
					DocumentElement nestedObject = document.addObject( "foo_nested" );
					nestedObject.addValue( FIRSTNAME_FIELD, null );
					nestedObject.addValue( LASTNAME_FIELD, null );
					DocumentElement flattenedObject = document.addObject( "foo_flattened" );
					flattenedObject.addValue( FIRSTNAME_FIELD, null );
					flattenedObject.addValue( LASTNAME_FIELD, null );
				} )
				.join();

		// Check that documents are indexed and the dynamic object field can be detected through an exists() predicate
		assertThatQuery( query( f -> f.exists().field( "foo_nested" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), documentWhereObjectFieldExistsId );
		assertThatQuery( query( f -> f.exists().field( "foo_flattened" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), documentWhereObjectFieldExistsId );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		mapping.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		assertThatQuery( query( f -> f.exists().field( "foo_nested" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), documentWhereObjectFieldExistsId );
		assertThatQuery( query( f -> f.exists().field( "foo_flattened" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), documentWhereObjectFieldExistsId );
	}

	private SearchQuery<DocumentReference> query(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return index.createScope().query()
				.where( predicateContributor )
				.toQuery();
	}

	private void checkNested(String objectFieldPath) {
		assertThatQuery( query(
				f -> f.nested( objectFieldPath )
						.add( f.match().field( objectFieldPath + "." + FIRSTNAME_FIELD )
								.matching( FIRSTNAME_1 ) )
						.add( f.match().field( objectFieldPath + "." + LASTNAME_FIELD )
								.matching( LASTNAME_1 ) )
		) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_MATCHING_FOR_NESTED );
	}

	private void checkFlattened(String objectFieldPath) {
		assertThatThrownBy( () -> query(
				f -> f.nested( objectFieldPath )
						.add( f.match().field( objectFieldPath + "." + FIRSTNAME_FIELD )
								.matching( FIRSTNAME_1 ) )
						.add( f.match().field( objectFieldPath + "." + LASTNAME_FIELD )
								.matching( LASTNAME_1 ) )
		) )
				.hasMessageContainingAll( "Cannot use 'predicate:nested' on field '" + objectFieldPath + "'",
						"Some object field features require a nested structure; "
								+ "try setting the field structure to 'NESTED' and reindexing all your data" );
		assertThatQuery( query(
				f -> f.and()
						.add( f.match().field( objectFieldPath + "." + FIRSTNAME_FIELD )
								.matching( FIRSTNAME_1 ) )
						.add( f.match().field( objectFieldPath + "." + LASTNAME_FIELD )
								.matching( LASTNAME_1 ) )
		) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_MATCHING_FOR_NESTED, DOCUMENT_MATCHING_FOR_ALL );
	}

	private StubMapping setup(StubMappingSchemaManagementStrategy schemaManagementStrategy,
			Consumer<IndexSchemaElement> rootTemplatesBinder) {
		return setup( schemaManagementStrategy, rootTemplatesBinder, ignored -> { } );
	}

	private StubMapping setup(StubMappingSchemaManagementStrategy schemaManagementStrategy,
			Consumer<IndexSchemaElement> rootTemplatesBinder,
			Consumer<IndexSchemaElement> staticObjectTemplatesBinder) {
		this.index = SimpleMappedIndex.of(
				root -> new IndexBinding( root, rootTemplatesBinder, staticObjectTemplatesBinder ) );

		return setupHelper.start().withIndex( index )
				.withSchemaManagement( schemaManagementStrategy )
				.setup();
	}

	private static class IndexBinding {
		final ObjectBinding staticObject;

		IndexBinding(IndexSchemaElement root, Consumer<IndexSchemaElement> rootTemplatesBinder,
				Consumer<IndexSchemaElement> staticObjectTemplatesBinder) {
			rootTemplatesBinder.accept( root );

			staticObject = new ObjectBinding( root, "staticObject", staticObjectTemplatesBinder );
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		private ObjectBinding(IndexSchemaElement parent, String relativeFieldName,
				Consumer<IndexSchemaElement> templatesBinder) {
			this.relativeFieldName = relativeFieldName;

			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.FLATTENED );
			self = objectField.toReference();

			templatesBinder.accept( objectField );
		}
	}
}
