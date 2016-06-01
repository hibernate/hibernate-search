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
import org.hibernate.search.exception.SearchException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class DurationBridgeTest {

	private static final long NANOS_PER_SECOND = 1000_000_000L;

	private static final DurationBridge BRIDGE = DurationBridge.INSTANCE;

	private static final Duration MAX_DURATION = Duration.ofNanos( Long.MAX_VALUE );
	private static final Duration MIN_DURATION = Duration.ofSeconds( Long.MIN_VALUE / NANOS_PER_SECOND, 999_999_999 );
	private static final Duration CUSTOM_DURATION = Duration.ofSeconds( -5808L, 10 );
	private static final Duration ZERO_DURATION = Duration.ZERO;
	private static final Duration OUT_OF_RANGE_DURATION = Duration.ofSeconds( Long.MAX_VALUE, 999_999_999L );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_DURATION ) ).isEqualTo( String.valueOf( MAX_DURATION.toNanos() ) );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_DURATION ) ).isEqualTo( String.valueOf( MIN_DURATION.toNanos() ) );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_DURATION ) ).isEqualTo( String.valueOf( CUSTOM_DURATION.toNanos() ) );
	}

	@Test
	public void testZeroObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( ZERO_DURATION ) ).isEqualTo( "0" );
	}

	@Test
	public void testExceptionValueTooBig() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000297" );

		BRIDGE.objectToString( OUT_OF_RANGE_DURATION );
	}
}
