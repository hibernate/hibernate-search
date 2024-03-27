/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.runtime.context.JobContext;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.CompanyGroup;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Person;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.HibernateSearchPartitionMapper;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.tenancy.spi.StringTenantIdentifierConverter;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Single-component test for partition plan validation.
 *
 * @author Mincong Huang
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateSearchPartitionMapperComponentIT {

	private static final int COMP_ROWS = 3;
	private static final int PERS_ROWS = 8;

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory emf;

	private JobContext mockedJobContext;

	private HibernateSearchPartitionMapper partitionMapper;

	@BeforeAll
	public void init() {
		emf = ormSetupHelper.start().withAnnotatedTypes( Company.class, Person.class, CompanyGroup.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.dataClearingIndexOnly()
				.setup();

		with( emf ).runInTransaction( session -> {
			for ( int i = 1; i <= COMP_ROWS; i++ ) {
				session.persist( new Company( "C" + i ) );
			}
			for ( int i = 1; i <= PERS_ROWS; i++ ) {
				session.persist( new Person( "P" + i, "", "" ) );
			}
		} );

		final String maxThreads = String.valueOf( 1 );
		final String rowsPerPartition = String.valueOf( 3 );

		mockedJobContext = mock( JobContext.class );
		partitionMapper = new HibernateSearchPartitionMapper(
				null, null,
				maxThreads,
				null,
				rowsPerPartition,
				null,
				null,
				mockedJobContext
		);
	}

	/**
	 * Prove that there are N partitions for each root entity,
	 * where N stands for the ceiling number of the division
	 * between the rows to index and the max rows per partition.
	 */
	@Test
	void simple() throws Exception {
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		var companyType = JobTestUtil.createEntityTypeDescriptor( emf, Company.class );
		var personType = JobTestUtil.createEntityTypeDescriptor( emf, Person.class );
		jobData.setEntityTypeDescriptors( Arrays.asList( companyType, personType ) );
		jobData.setTenancyConfiguration( TenancyConfiguration.create(
				BeanHolder.of( StringTenantIdentifierConverter.INSTANCE ),
				Optional.empty(),
				""
		) );
		when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();

		int compPartitions = 0;
		int persPartitions = 0;
		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( MassIndexingPartitionProperties.ENTITY_NAME );
			if ( entityName.equals( companyType.jpaEntityName() ) ) {
				compPartitions++;
			}
			if ( entityName.equals( personType.jpaEntityName() ) ) {
				persPartitions++;
			}
			/*
			 * The checkpoint interval should have defaulted to the value of rowsPerPartition,
			 * since the value of rowsPerPartition is lower than the static default for checkpoint interval.
			 */
			String checkpointInterval = p.getProperty( MassIndexingPartitionProperties.CHECKPOINT_INTERVAL );
			assertThat( checkpointInterval ).isNotNull();
			assertThat( checkpointInterval ).isEqualTo( "3" );
		}

		// nbPartitions = rows / rowsPerPartition
		assertThat( compPartitions ).isEqualTo( 1 ); // 3 / 3 => 1 partition
		assertThat( persPartitions ).isEqualTo( 3 ); // 8 / 3 => 3 partitions
	}

	@Test
	void noData() throws Exception {
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		var companyGroupType = JobTestUtil.createEntityTypeDescriptor( emf, CompanyGroup.class );
		jobData.setEntityTypeDescriptors( Collections.singletonList( companyGroupType ) );
		jobData.setTenancyConfiguration( TenancyConfiguration.create(
				BeanHolder.of( StringTenantIdentifierConverter.INSTANCE ),
				Optional.empty(),
				""
		) );
		when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();

		int compGroupPartitions = 0;
		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( MassIndexingPartitionProperties.ENTITY_NAME );
			if ( entityName.equals( companyGroupType.jpaEntityName() ) ) {
				compGroupPartitions++;
			}
		}

		// Did not find anything in the ResultSet at index "rowsPerPartition"
		// => 1 partition covering the whole range.
		// We'll notice there is no data later, when reading IDs to reindex.
		assertThat( compGroupPartitions ).isEqualTo( 1 );
	}
}
