/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.junit.Assert.fail;

import java.time.Instant;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class MassIndexingConditionalExpressionsIT {

	// where INSTANT_0 < INSTANT_1 < INSTANT_2
	private static final Instant INSTANT_0 = Instant.ofEpochMilli( 1_000_000 );
	private static final Instant INSTANT_1 = Instant.ofEpochMilli( 1_500_000 );
	private static final Instant INSTANT_2 = Instant.ofEpochMilli( 2_000_000 );

	// where INT_0 < INT_1 < INT_2
	private static final int INT_0 = 0;
	private static final int INT_1 = 1;
	private static final int INT_2 = 2;

	private static final String KEYWORD_A_1 = "a-1";
	private static final String KEYWORD_A_2 = "a-2";
	private static final String KEYWORD_B_1 = "b-1";
	private static final String KEYWORD_B_2 = "b-2";

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( H0_Indexed.NAME );
		backendMock.expectAnySchema( H1_B_Indexed.NAME );
		backendMock.expectAnySchema( H2_Root_Indexed.NAME );
		backendMock.expectAnySchema( H2_A_C_Indexed.NAME );
		backendMock.expectAnySchema( H2_B_Indexed.NAME );
		backendMock.expectAnySchema( H3_A_Indexed.NAME );
		backendMock.expectAnySchema( H3_B_Indexed.NAME );

		setupContext.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLED, false )
				.withAnnotatedTypes(
						H0_Indexed.class,
						H1_Root_NotIndexed.class, H1_A_NotIndexed.class, H1_B_Indexed.class,
						H2_Root_Indexed.class,
						H2_A_NotIndexed.class, H2_A_C_Indexed.class,
						H2_B_Indexed.class, H2_B_D_NotIndexed.class,
						H3_Root.class, H3_A_Indexed.class, H3_B_Indexed.class
				);
	}

	@Before
	public void initData() {
		setupHolder.runInTransaction( session -> {
			session.persist( new H0_Indexed( 1, INSTANT_0, INT_0 ) );
			session.persist( new H0_Indexed( 2, INSTANT_0, INT_2 ) );
			session.persist( new H0_Indexed( 3, INSTANT_2, INT_0 ) );
			session.persist( new H0_Indexed( 4, INSTANT_2, INT_2 ) );

			session.persist( new H1_Root_NotIndexed( 1, INSTANT_0, INT_0 ) );
			session.persist( new H1_Root_NotIndexed( 2, INSTANT_0, INT_2 ) );
			session.persist( new H1_Root_NotIndexed( 3, INSTANT_2, INT_0 ) );
			session.persist( new H1_Root_NotIndexed( 4, INSTANT_2, INT_2 ) );
			session.persist( new H1_A_NotIndexed( 5, INSTANT_0, INT_0 ) );
			session.persist( new H1_A_NotIndexed( 6, INSTANT_0, INT_2 ) );
			session.persist( new H1_A_NotIndexed( 7, INSTANT_2, INT_0 ) );
			session.persist( new H1_A_NotIndexed( 8, INSTANT_2, INT_2 ) );
			session.persist( new H1_B_Indexed( 9, INSTANT_0, INT_0 ) );
			session.persist( new H1_B_Indexed( 10, INSTANT_0, INT_2 ) );
			session.persist( new H1_B_Indexed( 11, INSTANT_2, INT_0 ) );
			session.persist( new H1_B_Indexed( 12, INSTANT_2, INT_2 ) );

			session.persist( new H2_Root_Indexed( 1, INSTANT_0, INT_0 ) );
			session.persist( new H2_Root_Indexed( 2, INSTANT_0, INT_2 ) );
			session.persist( new H2_Root_Indexed( 3, INSTANT_2, INT_0 ) );
			session.persist( new H2_Root_Indexed( 4, INSTANT_2, INT_2 ) );
			session.persist( new H2_A_NotIndexed( 5, INSTANT_0, INT_0 ) );
			session.persist( new H2_A_NotIndexed( 6, INSTANT_0, INT_2 ) );
			session.persist( new H2_A_NotIndexed( 7, INSTANT_2, INT_0 ) );
			session.persist( new H2_A_NotIndexed( 8, INSTANT_2, INT_2 ) );
			session.persist( new H2_A_C_Indexed( 9, INSTANT_0, INT_0 ) );
			session.persist( new H2_A_C_Indexed( 10, INSTANT_0, INT_2 ) );
			session.persist( new H2_A_C_Indexed( 11, INSTANT_2, INT_0 ) );
			session.persist( new H2_A_C_Indexed( 12, INSTANT_2, INT_2 ) );
			session.persist( new H2_B_Indexed( 13, INSTANT_0, INT_0 ) );
			session.persist( new H2_B_Indexed( 14, INSTANT_0, INT_2 ) );
			session.persist( new H2_B_Indexed( 15, INSTANT_2, INT_0 ) );
			session.persist( new H2_B_Indexed( 16, INSTANT_2, INT_2 ) );
			session.persist( new H2_B_D_NotIndexed( 17, INSTANT_0, INT_0 ) );
			session.persist( new H2_B_D_NotIndexed( 18, INSTANT_0, INT_2 ) );
			session.persist( new H2_B_D_NotIndexed( 19, INSTANT_2, INT_0 ) );
			session.persist( new H2_B_D_NotIndexed( 20, INSTANT_2, INT_2 ) );

			session.persist( new H3_A_Indexed( 1L, KEYWORD_A_1 ) );
			session.persist( new H3_A_Indexed( 2L, KEYWORD_A_2 ) );
			session.persist( new H3_B_Indexed( 3L, KEYWORD_B_1 ) );
			session.persist( new H3_B_Indexed( 4L, KEYWORD_B_2 ) );
		} );
	}

	@Test
	public void noHierarchy() {
		setupHolder.runNoTransaction( session -> {
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
	public void noHierarchy_withParams() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H0_Indexed.class );
			indexer.type( H0_Indexed.class ).reindexOnly( "e.number < :number and e.moment > :moment" )
					.param( "moment", INSTANT_1 )
					.param( "number", INT_1 );

			backendMock.expectWorks( H0_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "text", "text3" ) );

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
		setupHolder.runNoTransaction( session -> {
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
		setupHolder.runNoTransaction( session -> {
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
		setupHolder.runNoTransaction( session -> {
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
		setupHolder.runNoTransaction( session -> {
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
		setupHolder.runNoTransaction( session -> {
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
	public void rootIndexed_someSubclassesIndexed_requestMassIndexingOnRoot_withParams() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class );

			indexer.type( H2_Root_Indexed.class ).reindexOnly( "e.rootNumber = :number and e.rootMoment > :moment" )
					.param( "number", INT_2 )
					.param( "moment", INSTANT_0 );

			indexer.type( H2_B_Indexed.class ).reindexOnly( "e.bNumber = :number and e.rootMoment < :moment" )
					.param( "number", INT_0 )
					.param( "moment", INSTANT_1 );

			backendMock.expectWorks( H2_Root_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "4", b -> b.field( "rootText", "text4" ) );
			backendMock.expectWorks( H2_A_C_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "aText", "text12" )
							.field( "cText", "text12" ) );
			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "13", b -> b
							.field( "rootText", "text13" )
							.field( "bText", "text13" ) );

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
		setupHolder.runNoTransaction( session -> {
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

	@Test
	public void sameFieldName_targetingEachClass() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H3_Root.class );

			indexer.type( H3_A_Indexed.class ).reindexOnly( "e.myProperty = :myProperty" )
					.param( "myProperty", KEYWORD_A_1 );
			indexer.type( H3_B_Indexed.class ).reindexOnly( "e.myProperty = :myProperty" )
					.param( "myProperty", KEYWORD_B_1 );

			backendMock.expectWorks( H3_A_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "myProperty", KEYWORD_A_1 ) );
			backendMock.expectWorks( H3_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "myProperty", KEYWORD_B_1 ) );

			backendMock.expectIndexScaleWorks( H3_A_Indexed.NAME, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H3_B_Indexed.NAME, session.getTenantIdentifier() )
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
	public void sameFieldName_targetingInterface() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H3_Root.class );

			indexer.type( H3_I.class ).reindexOnly( "e.myProperty = :p1 or e.myProperty = :p2" )
					.param( "p1", KEYWORD_A_1 )
					.param( "p2", KEYWORD_B_1 );

			backendMock.expectWorks( H3_A_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "myProperty", KEYWORD_A_1 ) );
			backendMock.expectWorks( H3_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3", b -> b.field( "myProperty", KEYWORD_B_1 ) );

			backendMock.expectIndexScaleWorks( H3_A_Indexed.NAME, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( H3_B_Indexed.NAME, session.getTenantIdentifier() )
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
	@TestForIssue(jiraKey = "HSEARCH-4266")
	public void orCondition_filterIndexedTypeOnly() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( H2_Root_Indexed.class );

			indexer.type( H2_Root_Indexed.class ).reindexOnly( "e.rootNumber = :number or e.rootMoment > :moment" )
					.param( "number", INT_2 )
					.param( "moment", INSTANT_0 );

			// If the OR condition was not handled properly, we would get two kinds of error:
			// 1. The mass indexer would try to index non-indexed entities, leading to errors which would make this test failing
			// 2. The mass indexer would try to index subclasses indexed entities more than once, leading to errors which would make this test failing.
			// Therefore, we can rely on this test to check that an `OR` condition is handled properly.
			backendMock.expectWorks( H2_Root_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "2", b -> b.field( "rootText", "text2" ) )
					.add( "3", b -> b.field( "rootText", "text3" ) )
					.add( "4", b -> b.field( "rootText", "text4" ) );
			backendMock.expectWorks( H2_A_C_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "10", b -> b.field( "rootText", "text10" )
							.field( "aText", "text10" )
							.field( "cText", "text10" ) )
					.add( "11", b -> b.field( "rootText", "text11" )
							.field( "aText", "text11" )
							.field( "cText", "text11" ) )
					.add( "12", b -> b.field( "rootText", "text12" )
							.field( "aText", "text12" )
							.field( "cText", "text12" ) );
			backendMock.expectWorks( H2_B_Indexed.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "14", b -> b
							.field( "rootText", "text14" )
							.field( "bText", "text14" ) )
					.add( "15", b -> b
							.field( "rootText", "text15" )
							.field( "bText", "text15" ) )
					.add( "16", b -> b
							.field( "rootText", "text16" )
							.field( "bText", "text16" ) );

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

	@Entity(name = H0_Indexed.NAME)
	@Indexed
	public static class H0_Indexed {
		public static final String NAME = "H0";

		@Id
		private Integer id;

		@GenericField
		private String text;

		private Instant moment;

		@Column(name = "numb")
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

	public interface H3_I {
		String getMyProperty();
	}

	@Entity(name = H3_Root.NAME)
	@Access(AccessType.PROPERTY)
	public abstract static class H3_Root {

		public static final String NAME = "H3_Root";

		private Long id;

		public H3_Root() {
		}

		public H3_Root(Long id) {
			this.id = id;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = H3_A_Indexed.NAME)
	@Access(AccessType.PROPERTY)
	@Indexed
	public static class H3_A_Indexed extends H3_Root implements H3_I {

		public static final String NAME = "H3_A";

		private String myProperty;

		public H3_A_Indexed() {
		}

		public H3_A_Indexed(Long id, String myProperty) {
			super( id );
			this.myProperty = myProperty;
		}

		@Override
		@KeywordField
		public String getMyProperty() {
			return myProperty;
		}

		public void setMyProperty(String myProperty) {
			this.myProperty = myProperty;
		}
	}

	@Entity(name = H3_B_Indexed.NAME)
	@Access(AccessType.PROPERTY)
	@Indexed
	public static class H3_B_Indexed extends H3_Root implements H3_I {

		public static final String NAME = "H3_B";

		private String myProperty;

		public H3_B_Indexed() {
		}

		public H3_B_Indexed(Long id, String myProperty) {
			super( id );
			this.myProperty = myProperty;
		}

		@Override
		@KeywordField
		public String getMyProperty() {
			return myProperty;
		}

		public void setMyProperty(String myProperty) {
			this.myProperty = myProperty;
		}
	}
}
