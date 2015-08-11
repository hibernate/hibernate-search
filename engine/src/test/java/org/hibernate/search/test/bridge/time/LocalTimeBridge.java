/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalTime;

import org.hibernate.search.bridge.builtin.time.impl.LocalTimeNumericBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class LocalTimeBridge {

	private static final LocalTimeNumericBridge BRIDGE = LocalTimeNumericBridge.INSTANCE;

	@Test
	public void testLocaTimeToLong() {
		LocalTime time = LocalTime.of( 16, 1, 59, 678_000_000 );
		assertThat( BRIDGE.encode( time ) ).isEqualTo( 16_01_59_678_000_000L );
	}

	@Test
	public void testBiggestLocaTimeToLong() {
		assertThat( BRIDGE.encode( LocalTime.MAX ) ).isEqualTo( 23_59_59_999_999_999L );
	}

	@Test
	public void testSmallestLocaTimeToLong() {
		assertThat( BRIDGE.encode( LocalTime.MIN ) ).isEqualTo( 0L );
	}

	@Test
	public void testLongToLocalTime() {
		LocalTime time = LocalTime.of( 16, 1, 59, 678_000_000 );
		assertThat( BRIDGE.decode( 16_01_59_678_000_000L ) ).isEqualTo( time );
	}

	@Test
	public void testLongToBiggestLocalTime() {
		assertThat( BRIDGE.decode( 23_59_59_999_999_999L ) ).isEqualTo( LocalTime.MAX );
	}

	@Test
	public void testIntToSmallestLocalTime() {
		assertThat( BRIDGE.decode( 0L ) ).isEqualTo( LocalTime.MIN );
	}
}
