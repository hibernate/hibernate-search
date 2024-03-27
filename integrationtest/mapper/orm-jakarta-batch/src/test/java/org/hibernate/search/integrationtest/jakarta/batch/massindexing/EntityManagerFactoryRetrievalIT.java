/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.jakarta.batch.util.HibernateSearchBatchTestConnectionProperties.connectionProperties;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Person;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.WhoAmI;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.integrationtest.jakarta.batch.util.PersistenceUnitTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Mincong Huang
 */
class EntityManagerFactoryRetrievalIT {

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	protected static final int INSTANCES_PER_DATA_TEMPLATE = 100;

	// We have three data templates per entity type (see setup)
	protected static final int INSTANCE_PER_ENTITY_TYPE = INSTANCES_PER_DATA_TEMPLATE * 3;

	/*
	 * Make sure to have more than one checkpoint,
	 * because we had errors related to that in the past.
	 */
	private static final int CHECKPOINT_INTERVAL = 10;

	private static final String SESSION_FACTORY_NAME = "primary_session_factory";

	protected EntityManagerFactory emf;

	@BeforeEach
	void setup() {
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

		emf = Persistence.createEntityManagerFactory( getPersistenceUnitName(), connectionProperties() );
		with( emf ).runInTransaction( em -> {
			companies.forEach( em::persist );
			people.forEach( em::persist );
			whos.forEach( em::persist );
		} );
	}

	@AfterEach
	void shutdown() {
		if ( emf != null ) {
			emf.close();
		}
	}

	protected String getPersistenceUnitName() {
		return PERSISTENCE_UNIT_NAME;
	}

	@Test
	void defaultNamespace() throws Exception {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertThat( companies ).isEmpty();

		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.entityManagerFactoryReference( getPersistenceUnitName() )
						.build()
		);

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertThat( companies ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	@Test
	void persistenceUnitNamespace() throws Exception {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertThat( companies ).isEmpty();

		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.entityManagerFactoryNamespace( "persistence-unit-name" )
						.entityManagerFactoryReference( getPersistenceUnitName() )
						.build()
		);

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertThat( companies ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

	@Test
	void sessionFactoryNamespace() throws Exception {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertThat( companies ).isEmpty();

		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.entityManagerFactoryNamespace( "session-factory-name" )
						.entityManagerFactoryReference( SESSION_FACTORY_NAME )
						.build()
		);

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertThat( companies ).hasSize( INSTANCES_PER_DATA_TEMPLATE );
	}

}
