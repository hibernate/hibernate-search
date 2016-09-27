/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.MonthDay;

import org.hibernate.search.bridge.builtin.time.impl.MonthDayBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class MonthDayBridgeTest {
	private static final MonthDayBridge BRIDGE = MonthDayBridge.INSTANCE;

	private static final MonthDay MAX_VALUE = MonthDay.of( 12, 31 );
	private static final MonthDay MIN_VALUE = MonthDay.of( 1, 1 );
	private static final MonthDay CUSTOM_VALUE = MonthDay.of( 1, 4 );

	private static final String MAX = "1231";
	private static final String MIN = "0101";
	private static final String CST = "0104";

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
