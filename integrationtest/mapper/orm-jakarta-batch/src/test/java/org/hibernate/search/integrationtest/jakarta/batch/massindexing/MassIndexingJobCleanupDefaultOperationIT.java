/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.CompanyGroup;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Person;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.WhoAmI;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingJobCleanupDefaultOperationIT {

	protected static final int INSTANCES_PER_DATA_TEMPLATE = 100;

	// We have three data templates per entity type (see setup)
	protected static final int INSTANCE_PER_ENTITY_TYPE = INSTANCES_PER_DATA_TEMPLATE * 3;

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory emf;

	@ParameterizedTest
	@EnumSource(MassIndexingDefaultCleanOperation.class)
	void smoke(MassIndexingDefaultCleanOperation operation) throws InterruptedException {
		emf = ormSetupHelper.start().withAnnotatedTypes( Company.class, Person.class, WhoAmI.class, CompanyGroup.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.withProperty( HibernateOrmMapperSettings.INDEXING_MASS_DEFAULT_CLEAN_OPERATION, operation )
				.setup();

		List<Company> companies = new ArrayList<>();
		for ( int i = 0; i < INSTANCE_PER_ENTITY_TYPE; i += 3 ) {
			int index1 = i;
			int index2 = i + 1;
			int index3 = i + 2;
			companies.add( new Company( "Google " + index1 ) );
			companies.add( new Company( "Red Hat " + index2 ) );
			companies.add( new Company( "Microsoft " + index3 ) );
		}

		with( emf ).runInTransaction( session -> {
			companies.forEach( session::persist );
		} );


		int expectedCount = 10;

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isZero();
		indexSomeCompanies( expectedCount );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isEqualTo( expectedCount );

		/*
		 * Request a mass indexing with a filter matching nothing,
		 * which should effectively amount to only drop-create the schema.
		 */
		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.reindexOnly( "name like :name", Map.of( "name", "NEVER_MATCH" ) )
						.build()
		);

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, Company.class ) ).isZero();
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
