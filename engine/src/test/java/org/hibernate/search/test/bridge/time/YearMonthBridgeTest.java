/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Year;
import java.time.YearMonth;

import org.hibernate.search.bridge.builtin.time.impl.YearMonthBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class YearMonthBridgeTest {
	private static final YearMonthBridge BRIDGE = YearMonthBridge.INSTANCE;

	private static final YearMonth MAX_VALUE = YearMonth.of( Year.MAX_VALUE, 12 );
	private static final YearMonth MIN_VALUE = YearMonth.of( Year.MIN_VALUE, 12 );
	private static final YearMonth CUSTOM_VALUE = YearMonth.of( -12434, 1 );

	private static final String MAX = "+99999999912";
	private static final String MIN = "-99999999912";
	private static final String CST = "-00001243401";

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_VALUE ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_VALUE ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_VALUE ) ).isEqualTo( CST );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( MAX_VALUE );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( MIN_VALUE );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CST ) ).isEqualTo( CUSTOM_VALUE );
	}
}
