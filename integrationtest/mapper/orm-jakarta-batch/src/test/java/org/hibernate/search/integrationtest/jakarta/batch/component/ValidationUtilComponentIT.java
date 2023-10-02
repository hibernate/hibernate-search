/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.component;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Person;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.ValidationUtil;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Mincong Huang
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidationUtilComponentIT {

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@BeforeAll
	void setup() {
		ormSetupHelper.start()
				.withAnnotatedTypes( Company.class, Person.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.setup();
	}

	@Test
	void validateEntityTypes_whenAllTypesAreAvailableInEMF() {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		ValidationUtil.validateEntityTypes( null, null, null, serializedEntityTypes );
	}

	@Test
	void validateEntityTypes_whenContainingNonIndexedTypes() {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class, NotIndexed.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		assertThatThrownBy( () -> ValidationUtil.validateEntityTypes( null, null, null, serializedEntityTypes ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "The following selected entity types aren't indexable: "
						+ NotIndexed.class.getName() + ". Check whether they are annotated with '@Indexed'." );
	}

	@Test
	void validatePositive_valueIsNegative() {
		assertThatThrownBy( () -> ValidationUtil.validatePositive( "MyParameter", -1 ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void validatePositive_valueIsZero() {
		assertThatThrownBy( () -> ValidationUtil.validatePositive( "MyParameter", 0 ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void validatePositive_valueIsPositive() {
		ValidationUtil.validatePositive( "MyParameter", 1 );
		// ok
	}

	@Test
	void validateCheckpointInterval_lessThanRowsPerPartition() {
		ValidationUtil.validateCheckpointInterval( 99, 100 );
		// ok
	}

	@Test
	void validateCheckpointInterval_equalToRowsPerPartition() {
		ValidationUtil.validateCheckpointInterval( 100, 100 );
		// ok
	}

	@Test
	void validateCheckpointInterval_greaterThanRowsPerPartition() {
		assertThatThrownBy( () -> ValidationUtil.validateCheckpointInterval( 101, 100 ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void validateSessionClearInterval_lessThanCheckpointInterval() {
		ValidationUtil.validateEntityFetchSize( 99, 100 );
		// ok
	}

	@Test
	void validateSessionClearInterval_equalToCheckpointInterval() {
		ValidationUtil.validateEntityFetchSize( 100, 100 );
		// ok
	}

	@Test
	void validateSessionClearInterval_greaterThanCheckpointInterval() {
		assertThatThrownBy( () -> ValidationUtil.validateEntityFetchSize( 101, 100 ) )
				.isInstanceOf( SearchException.class );
	}

	private static class NotIndexed {
		private NotIndexed() {
			// Private constructor, do not use it.
		}
	}
}
