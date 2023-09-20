/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class SerializationUtilTest {

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
		assertThatThrownBy( () -> SerializationUtil.parseIntegerParameter( "My parameter", "1.0" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid value for job parameter 'My parameter': '1.0'." );
	}

	@Test
	public void deserializeInt_fromOther() throws Exception {
		assertThatThrownBy( () -> SerializationUtil.parseIntegerParameter( "My parameter", "foo" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid value for job parameter 'My parameter': 'foo'." );
	}

	@Test
	public void deserializeInt_missing() throws Exception {
		assertThatThrownBy( () -> SerializationUtil.parseIntegerParameter( "My parameter", null ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid value for job parameter 'My parameter': 'null'." );
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
			assertThatThrownBy( () -> SerializationUtil.parseBooleanParameterOptional( "My parameter", value, true ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value for job parameter 'My parameter': '" + value + "'." );
		}
	}
}
