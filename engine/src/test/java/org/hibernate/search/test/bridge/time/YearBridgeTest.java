/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Year;

import org.hibernate.search.bridge.builtin.time.impl.YearBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class YearBridgeTest {

	private static final YearBridge BRIDGE = YearBridge.INSTANCE;

	private static final Year MAX_VALUE = Year.of( Year.MAX_VALUE );
	private static final Year MIN_VALUE = Year.of( Year.MIN_VALUE );
	private static final Year CUSTOM_VALUE = Year.of( 12434 );
	private static final Year ZERO_VALUE = Year.of( 0 );

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_VALUE ) ).isEqualTo( String.valueOf( MAX_VALUE.getValue() ) );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_VALUE ) ).isEqualTo( String.valueOf( MIN_VALUE.getValue() ) );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_VALUE ) ).isEqualTo( String.valueOf( CUSTOM_VALUE.getValue() ) );
	}

	@Test
	public void testZeroObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( ZERO_VALUE ) ).isEqualTo( String.valueOf( ZERO_VALUE.getValue() ) );
	}
}
