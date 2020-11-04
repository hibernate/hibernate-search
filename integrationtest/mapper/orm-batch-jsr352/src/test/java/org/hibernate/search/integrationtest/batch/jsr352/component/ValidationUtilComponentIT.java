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
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.ValidationUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Person;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
import org.hibernate.search.util.common.SearchException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class ValidationUtilComponentIT {

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	private static final String EMF_SCOPE = "persistence-unit-name";

	private static EntityManagerFactory emf;

	@BeforeClass
	public static void setUp() {
		emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
	}

	@AfterClass
	public static void tearDown() {
		if ( emf != null ) {
			emf.close();
			emf = null;
		}
	}

	@Test
	public void validateEntityTypes_whenAllTypesAreAvailableInEMF() throws Exception {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		ValidationUtil.validateEntityTypes( null, EMF_SCOPE, PERSISTENCE_UNIT_NAME, serializedEntityTypes );
	}

	@Test
	public void validateEntityTypes_whenContainingNonIndexedTypes() throws Exception {
		String serializedEntityTypes = Stream
				.of( Company.class, Person.class, NotIndexed.class )
				.map( Class::getName )
				.collect( Collectors.joining( "," ) );

		assertThatThrownBy( () -> ValidationUtil.validateEntityTypes( null, EMF_SCOPE, PERSISTENCE_UNIT_NAME, serializedEntityTypes ) )
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
		ValidationUtil.validateSessionClearInterval( 99, 100 );
		// ok
	}

	@Test
	public void validateSessionClearInterval_equalToCheckpointInterval() {
		ValidationUtil.validateSessionClearInterval( 100, 100 );
		// ok
	}

	@Test(expected = SearchException.class)
	public void validateSessionClearInterval_greaterThanCheckpointInterval() throws Exception {
		ValidationUtil.validateSessionClearInterval( 101, 100 );
	}

	private static class NotIndexed {
		private NotIndexed() {
			// Private constructor, do not use it.
		}
	}
}
