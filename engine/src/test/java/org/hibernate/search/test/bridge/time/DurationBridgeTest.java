/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Duration;

import org.hibernate.search.bridge.builtin.time.impl.DurationBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class DurationBridgeTest {

	private static final DurationBridge BRIDGE = DurationBridge.INSTANCE;

	private static final Duration MAX_DURATION = Duration.ofSeconds( Long.MAX_VALUE, 999_999_999 );
	private static final Duration MIN_DURATION = Duration.ofSeconds( Long.MIN_VALUE, 0 );
	private static final Duration CUSTOM_DURATION = Duration.ofSeconds( -5807L, 10 );
	private static final Duration ZERO_DURATION = Duration.ZERO;

	private static final String MAX = "+9223372036854775807" + "999999999";
	private static final String MIN = "-9223372036854775808" + "000000000";
	private static final String CST = "-0000000000000005807" + "000000010";
	private static final String ZER = "+0000000000000000000" + "000000000";

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_DURATION ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_DURATION ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_DURATION ) ).isEqualTo( CST );
	}

	@Test
	public void testZeroObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( ZERO_DURATION ) ).isEqualTo( ZER );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( MAX_DURATION );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( MIN_DURATION );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CST ) ).isEqualTo( CUSTOM_DURATION );
	}

	@Test
	public void testZeroStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( ZER ) ).isEqualTo( ZERO_DURATION );
	}
}
