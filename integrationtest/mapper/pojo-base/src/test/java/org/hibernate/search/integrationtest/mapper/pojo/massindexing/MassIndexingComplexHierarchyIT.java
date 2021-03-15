/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import static org.assertj.core.api.Fail.fail;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingStrategies;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Test that the {@link MassIndexer} correctly indexes even complex entity hierarchies
 * where superclasses are indexed but not all of their subclasses, and vice-versa.
 */
public class MassIndexingComplexHierarchyIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private SearchMapping mapping;

	private Map<Integer, H1_Root_NotIndexed> h1map = new LinkedHashMap<>();
	private Map<Integer, H2_Root_Indexed> h2map = new LinkedHashMap<>();

	@Before
	public void setup() {
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );
		mapping = setupHelper.start()
				.setup(
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class
				);

		backendMock.verifyExpectationsMet();

		h1map.put( 1, new H1_Root_NotIndexed( 1 ) );
		h1map.put( 2, new H1_A_NotIndexed( 2 ) );
		h1map.put( 3, new H1_B_Indexed( 3 ) );

		h2map.put( 1, new H2_Root_Indexed( 1 ) );
		h2map.put( 2, new H2_A_NotIndexed( 2 ) );
		h2map.put( 3, new H2_A_C_Indexed( 3 ) );
		h2map.put( 4, new H2_B_Indexed( 4 ) );
		h2map.put( 5, new H2_B_D_NotIndexed( 5 ) );
	}

	@Test
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
					.field( "bText", "text3" ) )
					.createdThenExecuted();

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifier() )
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
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H1_B_Indexed.class );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
					.field( "bText", "text3" ) )
					.createdThenExecuted();

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, searchSession.tenantIdentifier() )
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
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		try ( SearchSession searchSession = createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class );

			backendMock.expectWorks( H2_Root_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "rootText", "text1" ) )
					.createdThenExecuted();
			backendMock.expectWorks( H2_A_C_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
					.field( "aText", "text3" )
					.field( "cText", "text3" ) )
					.createdThenExecuted();
			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" )
					.field( "bText", "text4" ) )
					.createdThenExecuted();

			backendMock.expectIndexScaleWorks( H2_Root_Indexed.NAME, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H2_A_C_Indexed.NAME, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifier() )
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
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		try ( SearchSession searchSession = createSession() ) {
			MassIndexer indexer = searchSession.massIndexer( H2_B_Indexed.class );

			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" )
					.field( "bText", "text4" ) )
					.createdThenExecuted();

			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, searchSession.tenantIdentifier() )
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

	private SearchSession createSession() {
		return mapping.createSessionWithOptions().loading( (o) -> {

			o.registerLoader( H1_Root_NotIndexed.class, (identifiers) -> {
				return identifiers.stream()
						.map( (identifier) -> h1map.get( (Integer) identifier ) )
						.collect( Collectors.toList() );
			} );
			o.registerLoader( H2_Root_Indexed.class, (identifiers) -> {
				return identifiers.stream()
						.map( (identifier) -> h2map.get( (Integer) identifier ) )
						.collect( Collectors.toList() );
			} );

			o.massIndexingLoadingStrategy( H1_Root_NotIndexed.class, JavaBeanIndexingStrategies.from( h1map ) );
			o.massIndexingLoadingStrategy( H2_Root_Indexed.class, JavaBeanIndexingStrategies.from( h2map ) );
		} ).build();
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
}
