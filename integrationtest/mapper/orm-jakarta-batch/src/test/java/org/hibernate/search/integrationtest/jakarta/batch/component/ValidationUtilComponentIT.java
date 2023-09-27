/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * @author Mincong Huang
 */
public class ValidationUtilComponentIT {

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( Company.class, Person.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false );
	}

	@Test
	public void validateEntityTypes_whenAllTypesAreAvailableInEMF() throws Exception {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		ValidationUtil.validateEntityTypes( null, null, null, serializedEntityTypes );
	}

	@Test
	public void validateEntityTypes_whenContainingNonIndexedTypes() throws Exception {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class, NotIndexed.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		assertThatThrownBy( () -> ValidationUtil.validateEntityTypes( null, null, null, serializedEntityTypes ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "The following selected entity types aren't indexable: "
						+ NotIndexed.class.getName() + ". Check whether they are annotated with '@Indexed'." );
	}

	@Test(expected = SearchException.class)
	public void validatePositive_valueIsNegative() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", -1 );
	}

	@Test(expected = SearchException.class)
	public void validatePositive_valueIsZero() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", 0 );
	}

	@Test
	public void validatePositive_valueIsPositive() throws Exception {
		ValidationUtil.validatePositive( "MyParameter", 1 );
		// ok
	}

	@Test
	public void validateCheckpointInterval_lessThanRowsPerPartition() throws Exception {
		ValidationUtil.validateCheckpointInterval( 99, 100 );
		// ok
	}

	@Test
	public void validateCheckpointInterval_equalToRowsPerPartition() {
		ValidationUtil.validateCheckpointInterval( 100, 100 );
		// ok
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_greaterThanRowsPerPartition() throws Exception {
		ValidationUtil.validateCheckpointInterval( 101, 100 );
	}

	@Test
	public void validateSessionClearInterval_lessThanCheckpointInterval() throws Exception {
		ValidationUtil.validateEntityFetchSize( 99, 100 );
		// ok
	}

	@Test
	public void validateSessionClearInterval_equalToCheckpointInterval() {
		ValidationUtil.validateEntityFetchSize( 100, 100 );
		// ok
	}

	@Test(expected = SearchException.class)
	public void validateSessionClearInterval_greaterThanCheckpointInterval() throws Exception {
		ValidationUtil.validateEntityFetchSize( 101, 100 );
	}

	private static class NotIndexed {
		private NotIndexed() {
			// Private constructor, do not use it.
		}
	}
}
