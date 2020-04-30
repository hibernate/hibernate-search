/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.dynamic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubEntityReference;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
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

			root.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.multiValued();
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_NESTED ), document -> {
			DocumentElement nestedObject = document.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
			nestedObject = document.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
		} );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_ALL ), document -> {
			DocumentElement nestedObject = document.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
			nestedObject = document.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
		} );
		indexingPlan.execute().join();

		// Check that documents are indexed and the dynamic fields can be searched
		checkNested( "foo" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
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

			root.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.multiValued();
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_NESTED ), document -> {
			DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

			DocumentElement nestedObject = staticObject.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
			nestedObject = staticObject.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
		} );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_ALL ), document -> {
			DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

			DocumentElement nestedObject = staticObject.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_2 );
			nestedObject = staticObject.addObject( "foo" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_2 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
		} );
		indexingPlan.execute().join();

		// Check that documents are indexed and the dynamic fields can be searched
		checkNested( "staticObject.foo" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		checkNested( "staticObject.foo" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void matchingPathGlob() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_nested" )
					.multiValued();
			root.objectFieldTemplate( "flattenedTemplate", ObjectFieldStorage.FLATTENED )
					.matchingPathGlob( "*_flattened" )
					.multiValued();
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_NESTED ), document -> {
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
		} );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_ALL ), document -> {
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
		} );
		indexingPlan.execute().join();

		// Check that documents are indexed and the dynamic fields can be searched
		checkNested( "foo_nested" );
		checkFlattened( "bar_flattened" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		checkNested( "foo_nested" );
		checkFlattened( "bar_flattened" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void matchingPathGlob_precedence_firstDeclared() {
		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_nested_object" )
					.multiValued();
			root.objectFieldTemplate( "flattenedTemplate", ObjectFieldStorage.FLATTENED )
					.matchingPathGlob( "*_object" )
					.multiValued();
			root.objectFieldTemplate( "ignoredTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_object" );
			root.fieldTemplate( "ignoredFieldTemplate", f -> f.asString() )
					.matchingPathGlob( "*_object" );
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_NESTED ), document -> {
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
		} );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_ALL ), document -> {
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
		} );
		indexingPlan.execute().join();

		// Check that dynamic fields have the correct storage type
		checkNested( "foo_nested_object" );
		checkFlattened( "flattened_object" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
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

			root.objectFieldTemplate( "flattenedTemplate", ObjectFieldStorage.FLATTENED )
					.matchingPathGlob( "*_object" )
					.multiValued();
		};
		Consumer<IndexSchemaElement> staticObjectTemplatesBinder = staticObject -> {
			staticObject.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_nested_object" )
					.multiValued();
		};

		SearchIntegration integration = setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_NESTED ), document -> {
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
		} );
		indexingPlan.add( referenceProvider( DOCUMENT_MATCHING_FOR_ALL ), document -> {
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
		} );
		indexingPlan.execute().join();

		// Check that dynamic fields have the correct storage type
		checkNested( "staticObject.foo_nested_object" );
		checkFlattened( "staticObject.flattened_object" );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		checkNested( "staticObject.foo_nested_object" );
		checkFlattened( "staticObject.flattened_object" );
	}

	/**
	 * The {@code exists} predicate should detect static object fields even if all their sub-fields are dynamic.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void exists_staticObjectField() {
		assumeBackendSupportsDynamicChildFieldsInExistsPredicate();

		Consumer<IndexSchemaElement> rootTemplatesBinder = root -> { };
		Consumer<IndexSchemaElement> staticObjectTemplatesBinder = staticObject -> {
			staticObject.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			staticObject.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_nested" )
					.multiValued();
			staticObject.objectFieldTemplate( "flattenedTemplate", ObjectFieldStorage.FLATTENED )
					.matchingPathGlob( "*_flattened" )
					.multiValued();
		};

		SearchIntegration integration = setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		String documentWhereObjectFieldExistsId = "existing";
		String documentWhereObjectFieldDoesNotExistId = "not-existing";

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( documentWhereObjectFieldExistsId ), document -> {
			DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

			DocumentElement nestedObject = staticObject.addObject( "foo_nested" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
			DocumentElement flattenedObject = staticObject.addObject( "foo_flattened" );
			flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
		} );
		indexingPlan.add( referenceProvider( documentWhereObjectFieldDoesNotExistId ), document -> {
			DocumentElement staticObject = document.addObject( index.binding().staticObject.self );

			DocumentElement nestedObject = staticObject.addObject( "foo_nested" );
			nestedObject.addValue( FIRSTNAME_FIELD, null );
			nestedObject.addValue( LASTNAME_FIELD, null );
			DocumentElement flattenedObject = staticObject.addObject( "foo_flattened" );
			flattenedObject.addValue( FIRSTNAME_FIELD, null );
			flattenedObject.addValue( LASTNAME_FIELD, null );
		} );
		indexingPlan.execute().join();

		// Check that documents are indexed and the dynamic object field can be detected through an exists() predicate
		SearchResultAssert.assertThat( query( f -> f.exists().field( "staticObject" ) ) )
				.hasDocRefHitsAnyOrder( index.name(), documentWhereObjectFieldExistsId );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY,
				rootTemplatesBinder, staticObjectTemplatesBinder );

		SearchResultAssert.assertThat( query( f -> f.exists().field( "staticObject" ) ) )
				.hasDocRefHitsAnyOrder( index.name(), documentWhereObjectFieldExistsId );
	}

	/**
	 * The {@code exists} predicate should detect dynamic object fields even if all their sub-fields are dynamic.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void exists_dynamicObjectField() {
		assumeBackendSupportsDynamicChildFieldsInExistsPredicate();

		Consumer<IndexSchemaElement> templatesBinder = root -> {
			root.fieldTemplate( "fieldTemplate", f -> f.asString() )
					.matchingPathGlob( VALUE_FIELD_PATH_GLOB );

			root.objectFieldTemplate( "nestedTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_nested" )
					.multiValued();
			root.objectFieldTemplate( "flattenedTemplate", ObjectFieldStorage.FLATTENED )
					.matchingPathGlob( "*_flattened" )
					.multiValued();
		};

		SearchIntegration integration =
				setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY, templatesBinder );

		String documentWhereObjectFieldExistsId = "existing";
		String documentWhereObjectFieldDoesNotExistId = "not-existing";

		// Index a few documents
		IndexIndexingPlan<StubEntityReference> indexingPlan = index.createIndexingPlan();
		indexingPlan.add( referenceProvider( EMPTY ), document -> { } );
		indexingPlan.add( referenceProvider( documentWhereObjectFieldExistsId ), document -> {
			DocumentElement nestedObject = document.addObject( "foo_nested" );
			nestedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			nestedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
			DocumentElement flattenedObject = document.addObject( "foo_flattened" );
			flattenedObject.addValue( FIRSTNAME_FIELD, FIRSTNAME_1 );
			flattenedObject.addValue( LASTNAME_FIELD, LASTNAME_1 );
		} );
		indexingPlan.add( referenceProvider( documentWhereObjectFieldDoesNotExistId ), document -> {
			DocumentElement nestedObject = document.addObject( "foo_nested" );
			nestedObject.addValue( FIRSTNAME_FIELD, null );
			nestedObject.addValue( LASTNAME_FIELD, null );
			DocumentElement flattenedObject = document.addObject( "foo_flattened" );
			flattenedObject.addValue( FIRSTNAME_FIELD, null );
			flattenedObject.addValue( LASTNAME_FIELD, null );
		} );
		indexingPlan.execute().join();

		// Check that documents are indexed and the dynamic object field can be detected through an exists() predicate
		SearchResultAssert.assertThat( query( f -> f.exists().field( "foo_nested" ) ) )
				.hasDocRefHitsAnyOrder( index.name(), documentWhereObjectFieldExistsId );
		SearchResultAssert.assertThat( query( f -> f.exists().field( "foo_flattened" ) ) )
				.hasDocRefHitsAnyOrder( index.name(), documentWhereObjectFieldExistsId );

		// Try again with a clean Hibernate Search instance, where local schema caches are empty
		integration.close();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, templatesBinder );

		SearchResultAssert.assertThat( query( f -> f.exists().field( "foo_nested" ) ) )
				.hasDocRefHitsAnyOrder( index.name(), documentWhereObjectFieldExistsId );
		SearchResultAssert.assertThat( query( f -> f.exists().field( "foo_flattened" ) ) )
				.hasDocRefHitsAnyOrder( index.name(), documentWhereObjectFieldExistsId );
	}

	private void assumeBackendSupportsDynamicChildFieldsInExistsPredicate() {
		Assume.assumeTrue(
				"This backend doesn't take dynamic child fields into account when creating exists predicates on object fields.",
				TckConfiguration.get().getBackendFeatures().supportsDynamicChildFieldsInExistsPredicate()
		);
	}

	private SearchQuery<DocumentReference> query(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return index.createScope().query()
				.where( predicateContributor )
				.toQuery();
	}

	private void checkNested(String objectFieldPath) {
		SearchResultAssert.assertThat( query(
				f -> f.nested().objectField( objectFieldPath )
						.nest( f.bool()
								.must( f.match().field( objectFieldPath + "." + FIRSTNAME_FIELD )
										.matching( FIRSTNAME_1 ) )
								.must( f.match().field( objectFieldPath + "." + LASTNAME_FIELD )
										.matching( LASTNAME_1 ) )
						)
		) )
				.hasDocRefHitsAnyOrder( index.name(), DOCUMENT_MATCHING_FOR_NESTED );
	}

	private void checkFlattened(String objectFieldPath) {
		assertThatThrownBy( () -> query(
				f -> f.nested().objectField( objectFieldPath )
						.nest( f.bool()
								.must( f.match().field( objectFieldPath + "." + FIRSTNAME_FIELD )
										.matching( FIRSTNAME_1 ) )
								.must( f.match().field( objectFieldPath + "." + LASTNAME_FIELD )
										.matching( LASTNAME_1 ) )
						)
		) )
				.hasMessageContaining( "is not stored as nested" );
		SearchResultAssert.assertThat( query(
				f -> f.bool()
						.must( f.match().field( objectFieldPath + "." + FIRSTNAME_FIELD )
								.matching( FIRSTNAME_1 ) )
						.must( f.match().field( objectFieldPath + "." + LASTNAME_FIELD )
								.matching( LASTNAME_1 ) )
		) )
				.hasDocRefHitsAnyOrder( index.name(), DOCUMENT_MATCHING_FOR_NESTED, DOCUMENT_MATCHING_FOR_ALL );
	}

	private SearchIntegration setup(StubMappingSchemaManagementStrategy schemaManagementStrategy,
			Consumer<IndexSchemaElement> rootTemplatesBinder) {
		return setup( schemaManagementStrategy, rootTemplatesBinder, ignored -> { } );
	}

	private SearchIntegration setup(StubMappingSchemaManagementStrategy schemaManagementStrategy,
			Consumer<IndexSchemaElement> rootTemplatesBinder,
			Consumer<IndexSchemaElement> staticObjectTemplatesBinder) {
		this.index = SimpleMappedIndex.of( "MainIndex",
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

			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectFieldStorage.FLATTENED );
			self = objectField.toReference();

			templatesBinder.accept( objectField );
		}
	}
}
