/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.junit.Assert.fail;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
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

public class MassIndexingConditionalExpressionsIT {

	// where T0 < T1 < T2
	private static final Instant T0 = Instant.ofEpochMilli( 1_000_000 );
	private static final Instant T1 = Instant.ofEpochMilli( 1_500_000 );
	private static final Instant T2 = Instant.ofEpochMilli( 2_000_000 );

	// where I0 < I1 < I2
	private static final int I0 = 0;
	private static final int I1 = 1;
	private static final int I2 = 2;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( H0_Indexed.NAME );
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );
		sessionFactory = ormSetupHelper.start()
				.withPropertyRadical(
						HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY,
						AutomaticIndexingStrategyNames.NONE
				)
				.setup(
						H0_Indexed.class,
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class
				);
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new H0_Indexed( 1, T0, I0 ) );
			session.persist( new H0_Indexed( 2, T0, I2 ) );
			session.persist( new H0_Indexed( 3, T2, I0 ) );
			session.persist( new H0_Indexed( 4, T2, I2 ) );

			session.persist( new H1_Root_NotIndexed( 1, T0, I0 ) );
			session.persist( new H1_Root_NotIndexed( 2, T0, I2 ) );
			session.persist( new H1_Root_NotIndexed( 3, T2, I0 ) );
			session.persist( new H1_Root_NotIndexed( 4, T2, I2 ) );
			session.persist( new H1_A_NotIndexed( 5, T0, I0 ) );
			session.persist( new H1_A_NotIndexed( 6, T0, I2 ) );
			session.persist( new H1_A_NotIndexed( 7, T2, I0 ) );
			session.persist( new H1_A_NotIndexed( 8, T2, I2 ) );
			session.persist( new H1_B_Indexed( 9, T0, I0 ) );
			session.persist( new H1_B_Indexed( 10, T0, I2 ) );
			session.persist( new H1_B_Indexed( 11, T2, I0 ) );
			session.persist( new H1_B_Indexed( 12, T2, I2 ) );

			session.persist( new H2_Root_Indexed( 1, T0, I0 ) );
			session.persist( new H2_Root_Indexed( 2, T0, I2 ) );
			session.persist( new H2_Root_Indexed( 3, T2, I0 ) );
			session.persist( new H2_Root_Indexed( 4, T2, I2 ) );
			session.persist( new H2_A_NotIndexed( 5, T0, I0 ) );
			session.persist( new H2_A_NotIndexed( 6, T0, I2 ) );
			session.persist( new H2_A_NotIndexed( 7, T2, I0 ) );
			session.persist( new H2_A_NotIndexed( 8, T2, I2 ) );
			session.persist( new H2_A_C_Indexed( 9, T0, I0 ) );
			session.persist( new H2_A_C_Indexed( 10, T0, I2 ) );
			session.persist( new H2_A_C_Indexed( 11, T2, I0 ) );
			session.persist( new H2_A_C_Indexed( 12, T2, I2 ) );
			session.persist( new H2_B_Indexed( 13, T0, I0 ) );
			session.persist( new H2_B_Indexed( 14, T0, I2 ) );
			session.persist( new H2_B_Indexed( 15, T2, I0 ) );
			session.persist( new H2_B_Indexed( 16, T2, I2 ) );
			session.persist( new H2_B_D_NotIndexed( 17, T0, I0 ) );
			session.persist( new H2_B_D_NotIndexed( 18, T0, I2 ) );
			session.persist( new H2_B_D_NotIndexed( 19, T2, I0 ) );
			session.persist( new H2_B_D_NotIndexed( 20, T2, I2 ) );
		} );
	}

	@Test
	public void noHierarchy() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H0_Indexed.class );
			indexer.type( H0_Indexed.class ).reindexOnly( "e.number = 2" );

			backendMock.expectWorks( H0_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "2", b -> b.field( "text", "text2" ) )
					.add( "4", b -> b.field( "text", "text4" ) );

			backendMock.expectIndexScaleWorks( H0_Indexed.NAME, session.getTenantIdentifier() )
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
	}

	@Test
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot_conditionOnRoot() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );
			indexer.type( H1_Root_NotIndexed.class ).reindexOnly( "e.rootNumber = 2" );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "10", b -> b.field( "rootText", "text10" )
							.field( "bText", "text10" ) )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "bText", "text12" ) );

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
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot_conditionOnSubclass_rootField() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );
			indexer.type( H1_B_Indexed.class ).reindexOnly( "e.rootNumber = 2" );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "10", b -> b.field( "rootText", "text10" )
							.field( "bText", "text10" ) )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "bText", "text12" ) );

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
	public void rootNotIndexed_someSubclassesIndexed_requestMassIndexingOnRoot_conditionOnSubclass_subclassField() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H1_Root_NotIndexed.class );
			indexer.type( H1_B_Indexed.class ).reindexOnly( "e.bNumber = 2" );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "10", b -> b.field( "rootText", "text10" )
							.field( "bText", "text10" ) )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "bText", "text12" ) );

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
			indexer.type( H1_B_Indexed.class ).reindexOnly( "e.bNumber = 2" );

			backendMock.expectWorks( H1_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "10", b -> b.field( "rootText", "text10" )
							.field( "bText", "text10" ) )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "bText", "text12" ) );

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
			indexer.type( H2_Root_Indexed.class ).reindexOnly( "e.rootNumber = 2" );
			indexer.type( H2_B_Indexed.class ).reindexOnly( "e.rootNumber = 0" );

			backendMock.expectWorks( H2_Root_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "2", b -> b.field( "rootText", "text2" ) )
					.add( "4", b -> b.field( "rootText", "text4" ) );
			backendMock.expectWorks( H2_A_C_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "10", b -> b.field( "rootText", "text10" )
							.field( "aText", "text10" )
							.field( "cText", "text10" ) )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "aText", "text12" )
							.field( "cText", "text12" ) );
			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "13", b -> b
							.field( "rootText", "text13" )
							.field( "bText", "text13" ) )
					.add( "15", b -> b
							.field( "rootText", "text15" )
							.field( "bText", "text15" ) );

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
			indexer.type( H2_B_Indexed.class ).reindexOnly( "e.rootNumber = 2" );

			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "14", b -> b
							.field( "rootText", "text14" )
							.field( "bText", "text14" ) )
					.add( "16", b -> b
							.field( "rootText", "text16" )
							.field( "bText", "text16" ) );

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

	@Entity(name = H0_Indexed.NAME)
	@Indexed
	public static class H0_Indexed {
		public static final String NAME = "H0";

		@Id
		private Integer id;

		@GenericField
		private String text;

		private Instant moment;

		private Integer number;

		public H0_Indexed() {
		}

		public H0_Indexed(Integer id, Instant moment, Integer number) {
			this.id = id;
			this.text = "text" + id;
			this.moment = moment;
			this.number = number;
		}
	}

	@Entity(name = H1_Root_NotIndexed.NAME)
	public static class H1_Root_NotIndexed {

		public static final String NAME = "H1_Root";

		@Id
		private Integer id;

		@GenericField
		private String rootText;

		private Instant rootMoment;

		private Integer rootNumber;

		public H1_Root_NotIndexed() {
		}

		public H1_Root_NotIndexed(Integer id, Instant moment, Integer number) {
			this.id = id;
			this.rootText = "text" + id;
			this.rootMoment = moment;
			this.rootNumber = number;
		}
	}

	@Entity(name = H1_A_NotIndexed.NAME)
	public static class H1_A_NotIndexed extends H1_Root_NotIndexed {

		public static final String NAME = "H1_A";

		@GenericField
		private String aText;

		private Instant aMoment;

		private Integer aNumber;

		public H1_A_NotIndexed() {
		}

		public H1_A_NotIndexed(Integer id, Instant moment, Integer number) {
			super( id, moment, number );
			this.aText = "text" + id;
			this.aMoment = moment;
			this.aNumber = number;
		}
	}

	@Entity(name = H1_B_Indexed.NAME)
	@Indexed
	public static class H1_B_Indexed extends H1_Root_NotIndexed {

		public static final String NAME = "H1_B";

		@GenericField
		private String bText;

		private Instant bMoment;

		private Integer bNumber;

		public H1_B_Indexed() {
		}

		public H1_B_Indexed(Integer id, Instant moment, Integer number) {
			super( id, moment, number );
			this.bText = "text" + id;
			this.bMoment = moment;
			this.bNumber = number;
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

		private Instant rootMoment;

		private Integer rootNumber;

		public H2_Root_Indexed() {
		}

		public H2_Root_Indexed(Integer id, Instant moment, Integer number) {
			this.id = id;
			this.rootText = "text" + id;
			this.rootMoment = moment;
			this.rootNumber = number;
		}
	}

	@Entity(name = H2_A_NotIndexed.NAME)
	@Indexed(enabled = false)
	public static class H2_A_NotIndexed extends H2_Root_Indexed {

		public static final String NAME = "H2_A";

		@GenericField
		private String aText;

		private Instant aMoment;

		private Integer aNumber;

		public H2_A_NotIndexed() {
		}

		public H2_A_NotIndexed(Integer id, Instant moment, Integer number) {
			super( id, moment, number );
			this.aText = "text" + id;
			this.aMoment = moment;
			this.aNumber = number;
		}
	}

	@Entity(name = H2_B_Indexed.NAME)
	public static class H2_B_Indexed extends H2_Root_Indexed {

		public static final String NAME = "H2_B";

		@GenericField
		private String bText;

		private Instant bMoment;

		private Integer bNumber;

		public H2_B_Indexed() {
		}

		public H2_B_Indexed(Integer id, Instant moment, Integer number) {
			super( id, moment, number );
			this.bText = "text" + id;
			this.bMoment = moment;
			this.bNumber = number;
		}
	}

	@Entity(name = H2_A_C_Indexed.NAME)
	@Indexed
	public static class H2_A_C_Indexed extends H2_A_NotIndexed {

		public static final String NAME = "H2_A_C";

		@GenericField
		private String cText;

		private Instant cMoment;

		private Integer cNumber;

		public H2_A_C_Indexed() {
		}

		public H2_A_C_Indexed(Integer id, Instant moment, Integer number) {
			super( id, moment, number );
			this.cText = "text" + id;
			this.cMoment = moment;
			this.cNumber = number;
		}
	}

	@Entity(name = H2_B_D_NotIndexed.NAME)
	@Indexed(enabled = false)
	public static class H2_B_D_NotIndexed extends H2_B_Indexed {

		public static final String NAME = "H2_B_D";

		@GenericField
		private String dText;

		private Instant dMoment;

		private Integer dNumber;

		public H2_B_D_NotIndexed() {
		}

		public H2_B_D_NotIndexed(Integer id, Instant moment, Integer number) {
			super( id, moment, number );
			this.dText = "text" + id;
			this.dMoment = moment;
			this.dNumber = number;
		}
	}
}
