/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that the {@link MassIndexer} correctly indexes even complex entity hierarchies
 * where superclasses are indexed but not all of their subclasses, and vice-versa.
 */
class MassIndexingComplexHierarchyIT {

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );
		mapping = setupHelper.start()
				.expectCustomBeans()
				.setup(
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class
				);

		backendMock.verifyExpectationsMet();

		Map<Integer, H1_Root_NotIndexed> h1map = loadingContext.persistenceMap( H1_Root_NotIndexed.PERSISTENCE_KEY );
		h1map.put( 1, new H1_Root_NotIndexed( 1 ) );
		h1map.put( 2, new H1_A_NotIndexed( 2 ) );
		h1map.put( 3, new H1_B_Indexed( 3 ) );

		Map<Integer, H2_Root_Indexed> h2map = loadingContext.persistenceMap( H2_Root_Indexed.PERSISTENCE_KEY );
		h2map.put( 1, new H2_Root_Indexed( 1 ) );
		h2map.put( 2, new H2_A_NotIndexed( 2 ) );
		h2map.put( 3, new H2_A_C_Indexed( 3 ) );
		h2map.put( 4, new H2_B_Indexed( 4 ) );
		h2map.put( 5, new H2_B_D_NotIndexed( 5 ) );
	}

	@Test
	void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class )
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
							.field( "bText", "text3" ) );

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_B_Indexed.class )
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
							.field( "bText", "text3" ) );

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void rootIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class )
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			backendMock.expectWorks( H2_Root_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "rootText", "text1" ) );
			backendMock.expectWorks( H2_A_C_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
							.field( "aText", "text3" )
							.field( "cText", "text3" ) );
			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" )
							.field( "bText", "text4" ) );

			backendMock.expectIndexScaleWorks( H2_Root_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H2_A_C_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void rootIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_B_Indexed.class )
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" )
							.field( "bText", "text4" ) );

			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();
	}

	@SearchEntity(name = H1_Root_NotIndexed.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	public static class H1_Root_NotIndexed {

		public static final String NAME = "H1_Root_NotIndexed";
		public static final PersistenceTypeKey<H1_Root_NotIndexed, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( H1_Root_NotIndexed.class, Integer.class );

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
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed
	public static class H2_Root_Indexed {

		public static final String NAME = "H2_Root_Indexed";
		public static final PersistenceTypeKey<H2_Root_Indexed, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( H2_Root_Indexed.class, Integer.class );

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
}
