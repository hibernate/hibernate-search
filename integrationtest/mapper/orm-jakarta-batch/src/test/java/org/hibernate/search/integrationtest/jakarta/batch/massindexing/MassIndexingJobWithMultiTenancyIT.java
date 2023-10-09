/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil.findIndexedResultsInTenant;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Mincong Huang
 */
class MassIndexingJobWithMultiTenancyIT {

	private static final String TARGET_TENANT_ID = "targetTenant";

	private static final String UNUSED_TENANT_ID = "unusedTenant";

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);
	private SessionFactory sessionFactory;

	@BeforeEach
	public void setup() {
		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( Company.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.withBackendProperty( "multi_tenancy.strategy", "discriminator" )
				.tenants( TARGET_TENANT_ID, UNUSED_TENANT_ID )
				.setup();
		with( sessionFactory, TARGET_TENANT_ID )
				.runInTransaction( session -> companies.forEach( session::persist ) );
		with( sessionFactory, TARGET_TENANT_ID )
				.runNoTransaction( session -> Search.session( session ).workspace( Company.class ).purge() );
	}

	@Test
	void shouldHandleTenantIds() throws Exception {
		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.tenantId( TARGET_TENANT_ID )
						.build()
		);

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", TARGET_TENANT_ID ) )
				.hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", TARGET_TENANT_ID ) )
				.hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", TARGET_TENANT_ID ) )
				.hasSize( 1 );

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", UNUSED_TENANT_ID ) )
				.isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", UNUSED_TENANT_ID ) )
				.isEmpty();
	}
}
