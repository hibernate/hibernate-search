/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that the {@link MassIndexer} correctly indexes even complex entity hierarchies
 * where superclasses are indexed but not all of their subclasses, and vice-versa.
 */
public class MassIndexingComplexHierarchyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );

		sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY,
						AutomaticIndexingStrategyName.NONE )
				.setup(
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class
				);

		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new H1_Root_NotIndexed( 1 ) );
			session.persist( new H1_A_NotIndexed( 2 ) );
			session.persist( new H1_B_Indexed( 3 ) );
			session.persist( new H2_Root_Indexed( 1 ) );
			session.persist( new H2_A_NotIndexed( 2 ) );
			session.persist( new H2_A_C_Indexed( 3 ) );
			session.persist( new H2_B_Indexed( 4 ) );
			session.persist( new H2_B_D_NotIndexed( 5 ) );
		} );
	}

	@Test
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );

			backendMock.expectWorksAnyOrder( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
							.field( "bText", "text3" ) )
					.processedThenExecuted();

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, session.getTenantIdentifier() )
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
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H1_B_Indexed.class );

			backendMock.expectWorksAnyOrder( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
							.field( "bText", "text3" ) )
					.processedThenExecuted();

			backendMock.expectIndexScaleWorks( H1_B_Indexed.NAME, session.getTenantIdentifier() )
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
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnRoot() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class );

			backendMock.expectWorksAnyOrder( H2_Root_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "rootText", "text1" ) )
					.processedThenExecuted();
			backendMock.expectWorksAnyOrder( H2_A_C_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "rootText", "text3" )
							.field( "aText", "text3" )
							.field( "cText", "text3" ) )
					.processedThenExecuted();
			backendMock.expectWorksAnyOrder( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" )
							.field( "bText", "text4" ) )
					.processedThenExecuted();

			backendMock.expectIndexScaleWorks( H2_Root_Indexed.NAME, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H2_A_C_Indexed.NAME, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, session.getTenantIdentifier() )
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
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnIndexedSubclass() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H2_B_Indexed.class );

			backendMock.expectWorksAnyOrder( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" )
							.field( "bText", "text4" ) )
					.processedThenExecuted();

			backendMock.expectIndexScaleWorks( H2_B_Indexed.NAME, session.getTenantIdentifier() )
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
		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = H1_Root_NotIndexed.NAME)
	public static class H1_Root_NotIndexed {

		public static final String NAME = "H1_Root";

		@Id
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

	@Entity(name = H1_A_NotIndexed.NAME)
	public static class H1_A_NotIndexed extends H1_Root_NotIndexed {

		public static final String NAME = "H1_A";

		@GenericField
		private String aText;

		public H1_A_NotIndexed() {
		}

		public H1_A_NotIndexed(Integer id) {
			super( id );
			this.aText = "text" + id;
		}
	}

	@Entity(name = H1_B_Indexed.NAME)
	@Indexed
	public static class H1_B_Indexed extends H1_Root_NotIndexed {

		public static final String NAME = "H1_B";

		@GenericField
		private String bText;

		public H1_B_Indexed() {
		}

		public H1_B_Indexed(Integer id) {
			super( id );
			this.bText = "text" + id;
		}
	}

	@Entity(name = H2_Root_Indexed.NAME)
	@Indexed
	public static class H2_Root_Indexed {

		public static final String NAME = "H2_Root";

		@Id
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

	@Entity(name = H2_A_NotIndexed.NAME)
	@Indexed(enabled = false)
	public static class H2_A_NotIndexed extends H2_Root_Indexed {

		public static final String NAME = "H2_A";

		@GenericField
		private String aText;

		public H2_A_NotIndexed() {
		}

		public H2_A_NotIndexed(Integer id) {
			super( id );
			this.aText = "text" + id;
		}
	}

	@Entity(name = H2_B_Indexed.NAME)
	public static class H2_B_Indexed extends H2_Root_Indexed {

		public static final String NAME = "H2_B";

		@GenericField
		private String bText;

		public H2_B_Indexed() {
		}

		public H2_B_Indexed(Integer id) {
			super( id );
			this.bText = "text" + id;
		}
	}

	@Entity(name = H2_A_C_Indexed.NAME)
	@Indexed
	public static class H2_A_C_Indexed extends H2_A_NotIndexed {

		public static final String NAME = "H2_A_C";

		@GenericField
		private String cText;

		public H2_A_C_Indexed() {
		}

		public H2_A_C_Indexed(Integer id) {
			super( id );
			this.cText = "text" + id;
		}
	}

	@Entity(name = H2_B_D_NotIndexed.NAME)
	@Indexed(enabled = false)
	public static class H2_B_D_NotIndexed extends H2_B_Indexed {

		public static final String NAME = "H2_B_D";

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
