/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test that the {@link MassIndexer} correctly indexes even complex entity hierarchies
 * where superclasses are indexed but not all of their subclasses, and vice-versa.
 * It also tests {@link LoadingTypeGroup#includedTypesMap()},
 * and the position of the first item for the group super type.
 */
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class MassIndexingIncludedEntityMapHierarchyIT {

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_E_Indexed.NAME );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.setup(
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class, H2_B_E_Indexed.class
				);

		backendMock.verifyExpectationsMet();
	}

	@Test
	void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();

			assertThatThrownBy( indexer::startAndWait )
					.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
					.isInstanceOf( SimulatedFailure.class )
					.hasMessageStartingWith( H1_B_Indexed.NAME )
					.hasMessageNotContaining( H1_Root_NotIndexed.NAME )
					.hasMessageNotContaining( H1_A_NotIndexed.NAME )
					.hasMessageContaining( H1_B_Indexed.NAME )
					.hasMessageNotContaining( H2_Root_Indexed.NAME )
					.hasMessageNotContaining( H2_A_NotIndexed.NAME )
					.hasMessageNotContaining( H2_A_C_Indexed.NAME )
					.hasMessageNotContaining( H2_B_Indexed.NAME )
					.hasMessageNotContaining( H2_B_D_NotIndexed.NAME )
					.hasMessageNotContaining( H2_B_E_Indexed.NAME );
		}
	}

	@Test
	void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_B_Indexed.class );

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();

			assertThatThrownBy( indexer::startAndWait )
					.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
					.isInstanceOf( SimulatedFailure.class )
					.hasMessageStartingWith( H1_B_Indexed.NAME )
					.hasMessageNotContaining( H1_Root_NotIndexed.NAME )
					.hasMessageNotContaining( H1_A_NotIndexed.NAME )
					.hasMessageContaining( H1_B_Indexed.NAME )
					.hasMessageNotContaining( H2_Root_Indexed.NAME )
					.hasMessageNotContaining( H2_A_NotIndexed.NAME )
					.hasMessageNotContaining( H2_A_C_Indexed.NAME )
					.hasMessageNotContaining( H2_B_Indexed.NAME )
					.hasMessageNotContaining( H2_B_D_NotIndexed.NAME )
					.hasMessageNotContaining( H2_B_E_Indexed.NAME );
		}
	}

	@Test
	void rootIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class );

			backendMock.expectIndexScaleWorks( H2_Root_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_A_C_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_B_E_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();

			assertThatThrownBy( indexer::startAndWait )
					.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
					.isInstanceOf( SimulatedFailure.class )
					.hasMessageStartingWith( H2_Root_Indexed.NAME )
					.hasMessageNotContaining( H1_Root_NotIndexed.NAME )
					.hasMessageNotContaining( H1_A_NotIndexed.NAME )
					.hasMessageNotContaining( H1_B_Indexed.NAME )
					.hasMessageContaining( H2_Root_Indexed.NAME )
					.hasMessageNotContaining( H2_A_NotIndexed.NAME )
					.hasMessageContaining( H2_A_C_Indexed.NAME )
					.hasMessageContaining( H2_B_Indexed.NAME )
					.hasMessageNotContaining( H2_B_D_NotIndexed.NAME )
					.hasMessageContaining( H2_B_E_Indexed.NAME );
		}
	}

	@Test
	void rootIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_B_Indexed.class );

			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_B_E_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();

			assertThatThrownBy( indexer::startAndWait )
					.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
					.isInstanceOf( SimulatedFailure.class )
					.hasMessageStartingWith( H2_B_Indexed.NAME )
					.hasMessageNotContaining( H1_Root_NotIndexed.NAME )
					.hasMessageNotContaining( H1_A_NotIndexed.NAME )
					.hasMessageNotContaining( H1_B_Indexed.NAME )
					.hasMessageNotContaining( H2_Root_Indexed.NAME )
					.hasMessageNotContaining( H2_A_NotIndexed.NAME )
					.hasMessageNotContaining( H2_A_C_Indexed.NAME )
					.hasMessageContaining( H2_B_Indexed.NAME )
					.hasMessageNotContaining( H2_B_D_NotIndexed.NAME )
					.hasMessageContaining( H2_B_E_Indexed.NAME );
		}

	}

	public static class FailingMassIndexingStrategy<E> implements MassLoadingStrategy<E, Object> {
		public static class Binder implements EntityLoadingBinder {
			@Override
			public void bind(EntityLoadingBindingContext context) {
				context.massLoadingStrategy( context.entityType().rawType(), new FailingMassIndexingStrategy<>() );
			}
		}

		@Override
		public boolean equals(Object obj) {
			// All instances are the same
			return obj instanceof FailingMassIndexingStrategy;
		}

		@Override
		public int hashCode() {
			// All instances are the same
			return 1;
		}

		@Override
		public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<E> includedTypes,
				MassIdentifierSink<Object> sink, MassLoadingOptions options) {
			throw new SimulatedFailure( includedTypes.includedTypesMap().keySet().stream()
					.collect( Collectors.joining( "," ) ) );
		}

		@Override
		public MassEntityLoader<Object> createEntityLoader(LoadingTypeGroup<E> includedTypes, MassEntitySink<E> sink,
				MassLoadingOptions options) {
			return new MassEntityLoader<Object>() {
				@Override
				public void close() {
					// Nothing to do.
				}

				@Override
				public void load(List<Object> identifiers) {
					throw new UnsupportedOperationException( "Didn't expect this to be called" );
				}
			};
		}

	}

	@SearchEntity(name = H1_Root_NotIndexed.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = FailingMassIndexingStrategy.Binder.class))
	public static class H1_Root_NotIndexed {

		public static final String NAME = "H1_Root_NotIndexed";

		@DocumentId
		private Integer id;

		@GenericField
		private String rootText;

		public H1_Root_NotIndexed() {
		}

		public H1_Root_NotIndexed(Integer id) {
			this.id = id;
			this.rootText = "text" + id;
		}
	}

	@SearchEntity(name = H1_A_NotIndexed.NAME)
	public static class H1_A_NotIndexed extends H1_Root_NotIndexed {

		public static final String NAME = "H1_A_NotIndexed";

		@GenericField
		private String aText;

		public H1_A_NotIndexed() {
		}

		public H1_A_NotIndexed(Integer id) {
			super( id );
			this.aText = "text" + id;
		}
	}

	@SearchEntity(name = H1_B_Indexed.NAME)
	@Indexed
	public static class H1_B_Indexed extends H1_Root_NotIndexed {

		public static final String NAME = "H1_B_Indexed";

		@GenericField
		private String bText;

		public H1_B_Indexed() {
		}

		public H1_B_Indexed(Integer id) {
			super( id );
			this.bText = "text" + id;
		}
	}

	@SearchEntity(name = H2_Root_Indexed.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = FailingMassIndexingStrategy.Binder.class))
	@Indexed
	public static class H2_Root_Indexed {

		public static final String NAME = "H2_Root_Indexed";

		@DocumentId
		private Integer id;

		@GenericField
		private String rootText;

		public H2_Root_Indexed() {
		}

		public H2_Root_Indexed(Integer id) {
			this.id = id;
			this.rootText = "text" + id;
		}
	}

	@SearchEntity(name = H2_A_NotIndexed.NAME)
	@Indexed(enabled = false)
	public static class H2_A_NotIndexed extends H2_Root_Indexed {

		public static final String NAME = "H2_A_NotIndexed";

		@GenericField
		private String aText;

		public H2_A_NotIndexed() {
		}

		public H2_A_NotIndexed(Integer id) {
			super( id );
			this.aText = "text" + id;
		}
	}

	@SearchEntity(name = H2_B_Indexed.NAME)
	public static class H2_B_Indexed extends H2_Root_Indexed {

		public static final String NAME = "H2_B_Indexed";

		@GenericField
		private String bText;

		public H2_B_Indexed() {
		}

		public H2_B_Indexed(Integer id) {
			super( id );
			this.bText = "text" + id;
		}
	}

	@SearchEntity(name = H2_A_C_Indexed.NAME)
	@Indexed
	public static class H2_A_C_Indexed extends H2_A_NotIndexed {

		public static final String NAME = "H2_A_C_Indexed";

		@GenericField
		private String cText;

		public H2_A_C_Indexed() {
		}

		public H2_A_C_Indexed(Integer id) {
			super( id );
			this.cText = "text" + id;
		}
	}

	@SearchEntity(name = H2_B_D_NotIndexed.NAME)
	@Indexed(enabled = false)
	public static class H2_B_D_NotIndexed extends H2_B_Indexed {

		public static final String NAME = "H2_B_D_NotIndexed";

		@GenericField
		private String dText;

		public H2_B_D_NotIndexed() {
		}

		public H2_B_D_NotIndexed(Integer id) {
			super( id );
			this.dText = "text" + id;
		}
	}

	@SearchEntity(name = H2_B_E_Indexed.NAME)
	@Indexed(enabled = true)
	public static class H2_B_E_Indexed extends H2_B_Indexed {

		public static final String NAME = "H2_B_E_Indexed";

		@GenericField
		private String dText;

		public H2_B_E_Indexed() {
		}

		public H2_B_E_Indexed(Integer id) {
			super( id );
			this.dText = "text" + id;
		}
	}

	protected static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}

}
