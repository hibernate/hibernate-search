/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.component;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.ValidationUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Person;
import org.hibernate.search.integrationtest.batch.jsr352.util.BackendConfigurations;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Mincong Huang
 */
public class ValidationUtilComponentIT {

	@RegisterExtension
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );

	@RegisterExtension
	public Extension setupHolderMethodRule = setupHolder.methodExtension();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( Company.class, Person.class )
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false );
	}

	@Test
	void validateEntityTypes_whenAllTypesAreAvailableInEMF() throws Exception {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		ValidationUtil.validateEntityTypes( null, null, null, serializedEntityTypes );
	}

	@Test
	void validateEntityTypes_whenContainingNonIndexedTypes() throws Exception {
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
	void validatePositive_valueIsNegative() throws Exception {
		assertThatThrownBy( () -> ValidationUtil.validatePositive( "MyParameter", -1 ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void validatePositive_valueIsZero() throws Exception {
		assertThatThrownBy( () -> ValidationUtil.validatePositive( "MyParameter", 0 ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void validatePositive_valueIsPositive() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", 1 );
		// ok
	}

	@Test
	void validateCheckpointInterval_lessThanRowsPerPartition() throws Exception {
		ValidationUtil.validateCheckpointInterval( 99, 100 );
		// ok
	}

	@Test
	void validateCheckpointInterval_equalToRowsPerPartition() {
		ValidationUtil.validateCheckpointInterval( 100, 100 );
		// ok
	}

	@Test
	void validateCheckpointInterval_greaterThanRowsPerPartition() throws Exception {
		assertThatThrownBy( () -> ValidationUtil.validateCheckpointInterval( 101, 100 ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void validateSessionClearInterval_lessThanCheckpointInterval() throws Exception {
		ValidationUtil.validateSessionClearInterval( 99, 100 );
		// ok
	}

	@Test
	void validateSessionClearInterval_equalToCheckpointInterval() {
		ValidationUtil.validateSessionClearInterval( 100, 100 );
		// ok
	}

	@Test
	void validateSessionClearInterval_greaterThanCheckpointInterval() throws Exception {
		assertThatThrownBy( () -> ValidationUtil.validateSessionClearInterval( 101, 100 ) )
				.isInstanceOf( SearchException.class );
	}

	private static class NotIndexed {
		private NotIndexed() {
			// Private constructor, do not use it.
		}
	}
}
