/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.CompanyGroup;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Person;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.WhoAmI;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.StepProgress;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Mincong Huang
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingJobIT {

	protected static final int INSTANCES_PER_DATA_TEMPLATE = 100;

	// We have three data templates per entity type (see setup)
	protected static final int INSTANCE_PER_ENTITY_TYPE = INSTANCES_PER_DATA_TEMPLATE * 3;

	/*
	 * Make sure to have more than one checkpoint,
	 * because we had errors related to that in the past.
	 */
	private static final int CHECKPOINT_INTERVAL = INSTANCES_PER_DATA_TEMPLATE / 2;

	private static final String MAIN_STEP_NAME = "produceLuceneDoc";

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory emf;

	@BeforeAll
	void setup() {
		emf = ormSetupHelper.start().withAnnotatedTypes(
				Company.class, Person.class, WhoAmI.class, CompanyGroup.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.dataClearing( config -> config.clearOrder( CompanyGroup.class, Company.class ) )
				.setup();
	}

	@BeforeEach
	void initData() {
		List<Company> companies = new ArrayList<>();
		List<Person> people = new ArrayList<>();
		List<WhoAmI> whos = new ArrayList<>();
		for ( int i = 0; i < INSTANCE_PER_ENTITY_TYPE; i += 3 ) {
			int index1 = i;
			int index2 = i + 1;
			int index3 = i + 2;
			companies.add( new Company( "Google " + index1 ) );
			companies.add( new Company( "Red Hat " + index2 ) );
			companies.add( new Company( "Microsoft " + index3 ) );
			people.add( new Person( "BG " + index1, "Bill", "Gates" ) );
			people.add( new Person( "LT " + index2, "Linus", "Torvalds" ) );
			people.add( new Person( "SJ " + index3, "Steven", "Jobs" ) );
			whos.add( new WhoAmI( "cid01 " + index1, "id01 " + index1, "uid01 " + index1 ) );
			whos.add( new WhoAmI( "cid02 " + index2, "id02 " + index2, "uid02 " + index2 ) );
			whos.add( new WhoAmI( "cid03 " + index3, "id03 " + index3, "uid03 " + index3 ) );
		}

		with( emf ).runInTransaction( session -> {
			companies.forEach( session::persist );
			people.forEach( session::persist );
			whos.forEach( session::persist );
		} );

		with( emf ).runInTransaction( em -> {
			List<CompanyGroup> groups = new ArrayList<>();
			for ( int i = 0; i < INSTANCE_PER_ENTITY_TYPE; i += 3 ) {
				int index1 = i;
				int index2 = i + 1;
				int index3 = i + 2;
				groups.add( new CompanyGroup( "group" + index1, companies.get( index1 ) ) );
				groups.add( new CompanyGroup( "group" + index2, companies.get( index1 ), companies.get( index2 ) ) );
				groups.add( new CompanyGroup( "group" + index3, companies.get( index3 ) ) );
			}
			groups.forEach( em::persist );
		} );
	}

	@Test
	void simple() throws InterruptedException {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertThat( companies ).isEmpty();
		assertThat( people ).isEmpty();
		assertThat( whos ).isEmpty();

		JobExecution execution = JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.build()
		);
		assertProgress( execution, Person.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( execution, Company.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( execution, WhoAmI.class, INSTANCE_PER_ENTITY_TYPE );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertThat( companies ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( people ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( whos ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	@Test
	void simple_defaultCheckpointInterval() throws InterruptedException {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertThat( companies ).isEmpty();
		assertThat( people ).isEmpty();
		assertThat( whos ).isEmpty();

		JobExecution execution = JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.build()
		);
		assertProgress( execution, Person.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( execution, Company.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( execution, WhoAmI.class, INSTANCE_PER_ENTITY_TYPE );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertThat( companies ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( people ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( whos ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2637")
	void indexedEmbeddedCollection() throws InterruptedException {
		List<CompanyGroup> groupsContainingGoogle =
				JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		List<CompanyGroup> groupsContainingRedHat =
				JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		List<CompanyGroup> groupsContainingMicrosoft =
				JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertThat( groupsContainingGoogle ).isEmpty();
		assertThat( groupsContainingRedHat ).isEmpty();
		assertThat( groupsContainingMicrosoft ).isEmpty();

		JobExecution execution = JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntities( CompanyGroup.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.build()
		);
		assertProgress( execution, CompanyGroup.class, INSTANCE_PER_ENTITY_TYPE );

		groupsContainingGoogle = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		groupsContainingRedHat = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		groupsContainingMicrosoft = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertThat( groupsContainingGoogle ).hasSize( 2 * INSTANCES_PER_DATA_TEMPLATE );
		assertThat( groupsContainingRedHat ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( groupsContainingMicrosoft ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4487")
	void indexedEmbeddedCollection_idFetchSize_entityFetchSize_mysql() throws InterruptedException {
		Dialect dialect = emf.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
				.getJdbcEnvironment().getDialect();
		assumeTrue( "This test only makes sense on MySQL,"
				+ " which is the only JDBC driver that accepts (and, in a sense, requires)"
				+ " passing Integer.MIN_VALUE for the JDBC fetch size",
				dialect instanceof MySQLDialect && !( dialect instanceof MariaDBDialect ) );

		List<CompanyGroup> groupsContainingGoogle =
				JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		List<CompanyGroup> groupsContainingRedHat =
				JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		List<CompanyGroup> groupsContainingMicrosoft =
				JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertThat( groupsContainingGoogle ).isEmpty();
		assertThat( groupsContainingRedHat ).isEmpty();
		assertThat( groupsContainingMicrosoft ).isEmpty();

		JobExecution execution = JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntities( CompanyGroup.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						// For MySQL, this is the only way to get proper scrolling
						.idFetchSize( Integer.MIN_VALUE )
						.build()
		);
		assertProgress( execution, CompanyGroup.class, INSTANCE_PER_ENTITY_TYPE );

		groupsContainingGoogle = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		groupsContainingRedHat = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		groupsContainingMicrosoft = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertThat( groupsContainingGoogle ).hasSize( 2 * INSTANCES_PER_DATA_TEMPLATE );
		assertThat( groupsContainingRedHat ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( groupsContainingMicrosoft ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	@Test
	void purge() throws InterruptedException {
		int expectedCount = 10;

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isZero();
		indexSomeCompanies( expectedCount );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isEqualTo( expectedCount );

		/*
		 * Request a mass indexing with a filter matching nothing,
		 * which should effectively amount to a simple purge.
		 */
		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.purgeAllOnStart( true )
						.reindexOnly( "name like :name", Map.of( "name", "NEVER_MATCH" ) )
						.build()
		);

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isZero();
	}

	@Test
	void noPurge() throws InterruptedException {
		int expectedCount = 10;

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isZero();
		indexSomeCompanies( expectedCount );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isEqualTo( expectedCount );

		/*
		 * Request a mass indexing with a filter matching nothing, and requesting no purge at all,
		 * which should effectively amount to a no-op.
		 */
		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.purgeAllOnStart( false )
						.reindexOnly( "name like :name", Map.of( "name", "NEVER_MATCH" ) )
						.build()
		);

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isEqualTo( expectedCount );
	}

	@Test
	void reindexOnly() throws InterruptedException {
		// searches before mass index,
		// expected no results for each search
		assertThat( JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ) ).isEmpty();
		assertThat( JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ) ).isEmpty();
		assertThat( JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ) ).isEmpty();

		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.reindexOnly( "name like 'Google%' or name like 'Red Hat%'", Map.of() )
						.build()
		);

		assertThat( JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ) )
				.hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ) )
				.hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ) ).isEmpty();
	}

	@Test
	void reindexOnly_maxResults() throws InterruptedException {
		// searches before mass index,
		// expected no results for each search
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isZero();

		int maxResults = CHECKPOINT_INTERVAL + 1;

		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.reindexOnly( "name like 'Google%' or name like 'Red Hat%'", Map.of() )
						.maxResultsPerEntity( maxResults )
						.build()
		);

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isEqualTo( maxResults );
	}

	@Test
	void partitioned() throws InterruptedException {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertThat( companies ).isEmpty();
		assertThat( people ).isEmpty();
		assertThat( whos ).isEmpty();

		JobExecution execution = JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.rowsPerPartition( INSTANCE_PER_ENTITY_TYPE - 1 )
						.build()
		);
		assertProgress( execution, Person.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( execution, Company.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( execution, WhoAmI.class, INSTANCE_PER_ENTITY_TYPE );

		StepProgress progress = getMainStepProgress( execution );
		Map<Integer, Long> partitionProgress = progress.getPartitionProgress();
		assertThat( partitionProgress )
				.as( "Entities processed per partition" )
				.hasSize( 3 * 2 )
				.contains(
						// First entity type
						entry( 0, INSTANCE_PER_ENTITY_TYPE - 1L ), // Partition 1 for first entity type
						entry( 1, 1L ), // Partition 2 for first entity type
						// Second entity type
						entry( 2, INSTANCE_PER_ENTITY_TYPE - 1L ), // Partition 1 for second entity type
						entry( 3, 1L ), // Partition 2 for second entity type
						// Third entity type
						entry( 4, INSTANCE_PER_ENTITY_TYPE - 1L ), // Partition 1 for third entity type
						entry( 5, 1L ) // Partition 2 for third entity type
				);

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertThat( companies ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( people ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
		assertThat( whos ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	private void assertProgress(JobExecution execution, Class<?> entityType, int progressValue) {
		/*
		 * We cannot check the metrics, which in JBatch are set to 0
		 * for partitioned steps (the metrics are handled separately for
		 * each partition).
		 * Thus we check our own object.
		 */
		StepProgress progress = getMainStepProgress( execution );
		assertThat( progress.getEntityProgress() )
				.containsEntry(
						emf.getMetamodel().entity( entityType ).getName(),
						Long.valueOf( progressValue )
				);
	}

	private StepProgress getMainStepProgress(JobExecution execution) {
		List<StepExecution> stepExecutions = JobTestUtil.getOperator()
				.getStepExecutions( execution.getExecutionId() );
		for ( StepExecution stepExecution : stepExecutions ) {
			switch ( stepExecution.getStepName() ) {
				case MAIN_STEP_NAME:
					return (StepProgress) stepExecution.getPersistentUserData();
				default:
					break;
			}
		}
		throw new AssertionFailure( "Missing step progress for step '" + MAIN_STEP_NAME + "'" );
	}

	protected final void indexSomeCompanies(int count) {
		with( emf ).runInTransaction( em -> {
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Company> criteria = criteriaBuilder.createQuery( Company.class );
			Root<Company> root = criteria.from( Company.class );
			Path<Integer> id = root.get( root.getModel().getId( int.class ) );
			criteria.orderBy( criteriaBuilder.asc( id ) );
			List<Company> companies = em.createQuery( criteria ).setMaxResults( count ).getResultList();
			SearchSession session = Search.session( em );

			SearchIndexingPlan indexingPlan = session.indexingPlan();
			for ( Company company : companies ) {
				indexingPlan.addOrUpdate( company );
			}
		} );
	}
}
