/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Test that the {@link MassIndexer} correctly indexes even complex entity hierarchies
 * where superclasses are indexed but not all of their subclasses, and vice-versa.
 * It also tests {@link LoadingTypeGroup#includedTypesMap()},
 * and the position of the first item for the group super type.
 */
public class MassIndexingIncludedEntityMapHierarchyIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final StandalonePojoMappingSetupHelper setupHelper
			= StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_E_Indexed.NAME );

		FailingMassIndexingStrategy<H1_Root_NotIndexed> h1LoadingStrategy = failingStrategy();
		FailingMassIndexingStrategy<H2_Root_Indexed> h2LoadingStrategy = failingStrategy();
		mapping = setupHelper.start()
				.withConfiguration( b -> b
						.addEntityType( H1_Root_NotIndexed.class, c -> c.massLoadingStrategy( h1LoadingStrategy ) )
						.addEntityType( H2_Root_Indexed.class, c -> c.massLoadingStrategy( h2LoadingStrategy ) ) )
				.setup(
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class, H2_B_E_Indexed.class
				);

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifier() )
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
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_B_Indexed.class );

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifier() )
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
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class );

			backendMock.expectIndexScaleWorks( H2_Root_Indexed.NAME, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_A_C_Indexed.NAME, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_B_E_Indexed.NAME, searchSession.tenantIdentifier() )
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
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_B_Indexed.class );

			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments();
			backendMock.expectIndexScaleWorks( H2_B_E_Indexed.NAME, searchSession.tenantIdentifier() )
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

	public static <E> FailingMassIndexingStrategy<E> failingStrategy() {
		return new FailingMassIndexingStrategy<>();
	}

	public static class FailingMassIndexingStrategy<E> implements MassLoadingStrategy<E, Object> {

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
