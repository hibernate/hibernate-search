/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


/**
 * @author Mincong Huang
 */
public class SerializationUtilTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void serializeAndDeserialize() throws Exception {
		Object i = SerializationUtil.deserialize( SerializationUtil.serialize( 1 ) );
		assertThat( (Integer) i ).isEqualTo( 1 );
	}

	@Test
	public void deserializeInt_fromInt() throws Exception {
		int i = SerializationUtil.parseIntegerParameter( "My parameter", "1" );
		assertThat( i ).isEqualTo( 1 );
	}

	@Test
	public void deserializeInt_fromDouble() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH500029: Unable to parse value '1.0' for job parameter 'My parameter'." );

		SerializationUtil.parseIntegerParameter( "My parameter", "1.0" );
	}

	@Test
	public void deserializeInt_fromOther() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH500029: Unable to parse value 'foo' for job parameter 'My parameter'." );

		SerializationUtil.parseIntegerParameter( "My parameter", "foo" );
	}

	@Test
	public void deserializeInt_missing() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH500029: Unable to parse value 'null' for job parameter 'My parameter'." );

		SerializationUtil.parseIntegerParameter( "My parameter", null );
	}

	@Test
	public void deserializeInt_defaultValue() throws Exception {
		int i = SerializationUtil.parseIntegerParameterOptional( "My parameter", null, 1 );
		assertThat( i ).isEqualTo( 1 );
	}

	@Test
	public void deserializeBoolean_fromLowerCase() throws Exception {
		boolean t = SerializationUtil.parseBooleanParameterOptional( "My parameter 1", "true", false );
		boolean f = SerializationUtil.parseBooleanParameterOptional( "My parameter 2", "false", true );
		assertThat( t ).isTrue();
		assertThat( f ).isFalse();
	}

	@Test
	public void deserializeBoolean_fromUpperCase() throws Exception {
		boolean t = SerializationUtil.parseBooleanParameterOptional( "My parameter 1", "TRUE", false );
		boolean f = SerializationUtil.parseBooleanParameterOptional( "My parameter 2", "FALSE", true );
		assertThat( t ).isTrue();
		assertThat( f ).isFalse();
	}

	@Test
	public void deserializeBoolean_fromIrregularCase() throws Exception {
		boolean t = SerializationUtil.parseBooleanParameterOptional( "My parameter 1", "TruE", false );
		boolean f = SerializationUtil.parseBooleanParameterOptional( "My parameter 2", "FalSe", true );
		assertThat( t ).as( "Case should be ignored." ).isTrue();
		assertThat( f ).as( "Case should be ignored." ).isFalse();
	}

	@Test
	public void deserializeBoolean_fromMissing() throws Exception {
		boolean t = SerializationUtil.parseBooleanParameterOptional( "My parameter 1", null, true );
		boolean f = SerializationUtil.parseBooleanParameterOptional( "My parameter 2", null, false );
		assertThat( t ).as( "Default value should be returned." ).isTrue();
		assertThat( f ).as( "Default value should be returned." ).isFalse();
	}

	@Test
	public void deserializeBoolean_fromOthers() throws Exception {
		for ( String value : new String[] { "", "0", "1", "t", "f" } ) {
			try {
				SerializationUtil.parseBooleanParameterOptional( "My parameter", value, true );
				fail();
			}
			catch (SearchException e) {
				String expectedMsg = "HSEARCH500029: Unable to parse value '" + value + "' for job parameter 'My parameter'.";
				assertThat( e.getMessage() ).isEqualTo( expectedMsg );
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void deserializeCriteria() throws Exception {
		Criterion criterion1 = Restrictions.in( "keyA", "value1", "value2" );
		Criterion criterion2 = Restrictions.between( "keyB", "low", "high" );

		// Given a serialized criteria
		Set<Criterion> inputCriteria = new HashSet<>();
		inputCriteria.add( criterion1 );
		inputCriteria.add( criterion2 );
		String serializedCriteria = SerializationUtil.serialize( inputCriteria );

		// When it is deserialized
		Set<Criterion> actualCriteria = SerializationUtil
				.parseParameter( Set.class, MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA, serializedCriteria );

		// Then there're 2 criteria found.

		/*
		 * Hibernate ORM Issue:
		 *
		 * The implementations of interface 'org.hibernate.criterion.Criterion' haven't overridden the
		 * methods #equals and #hashCode, so the equality-by-value is actually equality-by-reference.
		 * Enable the following lines to see what happens.
		 */
//		assertThat( actualCriteria ).containsOnly( criterion1, criterion2 );

		Set<String> actualCriteriaAsStrings = actualCriteria.stream()
				.map( Object::toString )
				.collect( Collectors.toSet() );
		assertThat( actualCriteriaAsStrings ).containsOnly( criterion1.toString(), criterion2.toString() );
	}

}
