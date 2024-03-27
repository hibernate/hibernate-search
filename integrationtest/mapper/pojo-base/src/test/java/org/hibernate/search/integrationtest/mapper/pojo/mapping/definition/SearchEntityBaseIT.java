/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@SuppressWarnings("unused")
class SearchEntityBaseIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public StaticCounters staticCounters = StaticCounters.create();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-5068")
	void noInheritance() {
		@SearchEntity(name = "parentClassIndex")
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		@Indexed
		class IndexedEntitySubClass extends IndexedEntity {
		}
		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class, IndexedEntitySubClass.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntitySubClass.class.getName() )
						.failure( "this type is not an entity type" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-5068")
	void name_implicit() {
		@SearchEntity
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		backendMock.inLenientMode( () -> {
			var mapping = setupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();

			assertThat( mapping.allIndexedEntities() )
					.extracting( SearchIndexedEntity::name )
					.containsExactly( IndexedEntity.class.getSimpleName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-5068")
	void name_explicit() {
		@SearchEntity(name = "myEntityName")
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		backendMock.inLenientMode( () -> {
			var mapping = setupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();

			assertThat( mapping.allIndexedEntities() )
					.extracting( SearchIndexedEntity::name )
					.containsExactly( "myEntityName" );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	void name_noInheritance() {
		@SearchEntity(name = "parentName")
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;

			@GenericField
			String text;
		}
		@SearchEntity
		@Indexed
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			Integer number;
		}

		backendMock.inLenientMode( () -> {
			var mapping = setupHelper.start()
					.withAnnotatedTypes( IndexedEntity.class, IndexedEntitySubClass.class )
					.setup();

			assertThat( mapping.allIndexedEntities() )
					.extracting( SearchIndexedEntity::name )
					.containsExactlyInAnyOrder( "parentName", IndexedEntitySubClass.class.getSimpleName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-5068")
	void loadingBinder() {
		@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = StaticCounterLoadingBinder.class))
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
		}

		backendMock.inLenientMode( () -> {
			SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
			backendMock.verifyExpectationsMet();

			assertThat( staticCounters.get( StaticCounterLoadingBinder.KEY ) ).isEqualTo( 1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-5068")
	void loadingBinder_failure() {
		@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = FailingLoadingBinder.class))
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Simulated failure" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-5068")
	void loadingBinder_inheritance() {
		@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = StaticCounterLoadingBinder.class))
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		@SearchEntity
		@Indexed
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			Integer number;
		}

		backendMock.inLenientMode( () -> {
			SearchMapping mapping = setupHelper.start().expectCustomBeans()
					.withAnnotatedTypes( IndexedEntity.class, IndexedEntitySubClass.class )
					.setup();
			backendMock.verifyExpectationsMet();

			assertThat( staticCounters.get( StaticCounterLoadingBinder.KEY ) ).isEqualTo( 2 );
		} );
	}

	public static class StaticCounterLoadingBinder implements EntityLoadingBinder {
		private static final StaticCounters.Key KEY = StaticCounters.createKey();

		@Override
		public void bind(EntityLoadingBindingContext context) {
			assertThat( context ).isNotNull();
			assertThat( context.beanResolver() ).isNotNull();
			assertThat( context.entityType() ).isNotNull();
			assertThat( context.identifierType() ).isNotNull();
			context.selectionLoadingStrategy( Object.class, new SelectionLoadingStrategy<Object>() {
				@Override
				public SelectionEntityLoader<Object> createEntityLoader(LoadingTypeGroup<Object> includedTypes,
						SelectionLoadingOptions options) {
					throw new AssertionFailure( "This method should not be called in this test." );
				}
			} );
			context.massLoadingStrategy( Object.class, new MassLoadingStrategy<Object, Object>() {
				@Override
				public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<Object> includedTypes,
						MassIdentifierSink<Object> sink, MassLoadingOptions options) {
					throw new AssertionFailure( "This method should not be called in this test." );
				}

				@Override
				public MassEntityLoader<Object> createEntityLoader(LoadingTypeGroup<Object> includedTypes,
						MassEntitySink<Object> sink,
						MassLoadingOptions options) {
					throw new AssertionFailure( "This method should not be called in this test." );
				}
			} );
			StaticCounters.get().increment( KEY );
		}
	}

	public static class FailingLoadingBinder implements EntityLoadingBinder {
		@Override
		public void bind(EntityLoadingBindingContext context) {
			throw new RuntimeException( "Simulated failure" );
		}
	}
}
