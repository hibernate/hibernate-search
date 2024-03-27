/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil.findIndexedResultsInTenant;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Mincong Huang
 */
class MassIndexingJobWithMultiTenancyIT {

	public static List<? extends Arguments> params() {
		return List.of(
				Arguments.of( "TENANT 1", "TENANT 2", null ),
				Arguments.of( 1, 2, new TenantIdentifierConverter() {
					@Override
					public String toStringValue(Object tenantId) {
						return Objects.toString( tenantId, null );
					}

					@Override
					public Object fromStringValue(String tenantId) {
						return tenantId == null ? null : Integer.parseInt( tenantId );
					}
				} ),
				Arguments.of(
						UUID.fromString( "55555555-7777-6666-9999-000000000001" ),
						UUID.fromString( "55555555-7777-6666-9999-000000000002" ),
						new TenantIdentifierConverter() {
							@Override
							public String toStringValue(Object tenantId) {
								return Objects.toString( tenantId, null );
							}

							@Override
							public Object fromStringValue(String tenantId) {
								return tenantId == null ? null : UUID.fromString( tenantId );
							}
						}
				)
		);
	}

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);
	private SessionFactory sessionFactory;

	@ParameterizedTest
	@MethodSource("params")
	public void setup(Object tenant1, Object tenant2, TenantIdentifierConverter converter) throws Exception {
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start().withAnnotatedTypes( Company.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.withBackendProperty( "multi_tenancy.strategy", "discriminator" );
		if ( converter != null ) {
			setupContext.withProperty( HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER, converter );
		}
		sessionFactory = setupContext
				.tenantsWithHelperEnabled( tenant1, tenant2 )
				.setup();
		with( sessionFactory, tenant1 )
				.runInTransaction( session -> companies.forEach( session::persist ) );
		with( sessionFactory, tenant1 )
				.runNoTransaction( session -> Search.session( session ).workspace( Company.class ).purge() );

		JobTestUtil.startJobAndWaitForSuccessNoRetry(
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.tenantId( tenant1 )
						.build()
		);

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", tenant1 ) )
				.hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", tenant1 ) )
				.hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", tenant1 ) )
				.hasSize( 1 );

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", tenant2 ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", tenant2 ) )
				.isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", tenant2 ) )
				.isEmpty();
	}
}
