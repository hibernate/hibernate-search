/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Period;

import org.hibernate.search.bridge.builtin.time.impl.PeriodBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class PeriodBridgeTest {

	private static final PeriodBridge BRIDGE = PeriodBridge.INSTANCE;

	private static final Period MAX_PERIOD = Period.of( Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE );
	private static final Period MIN_PERIOD = Period.of( Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE );
	private static final Period ZERO_PERIOD = Period.ZERO;
	private static final Period CUSTOM_PERIOD = Period.of( 100, -20, 3 );

	private static final String MAX = "+2147483647" + "+2147483647" + "+2147483647";
	private static final String MIN = "-2147483648" + "-2147483648" + "-2147483648";
	private static final String ZER = "+0000000000" + "+0000000000" + "+0000000000";
	private static final String CST = "+0000000100" + "-0000000020" + "+0000000003";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_PERIOD ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_PERIOD ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_PERIOD ) ).isEqualTo( CST );
	}

	@Test
	public void testZeroObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( ZERO_PERIOD ) ).isEqualTo( ZER );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( MAX_PERIOD );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( MIN_PERIOD );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CST ) ).isEqualTo( CUSTOM_PERIOD );
	}

	@Test
	public void testZeroStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( ZER ) ).isEqualTo( ZERO_PERIOD );
	}
}
