/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.MonthDay;

import org.hibernate.search.bridge.builtin.time.impl.MonthDayNumericBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class MonthDayBridgeTest {

	private static final MonthDayNumericBridge BRIDGE = MonthDayNumericBridge.INSTANCE;

	@Test
	public void testMonthDayToInt() {
		assertThat( BRIDGE.encode( MonthDay.of( 4, 3 ) ) ).isEqualTo( 403 );
	}

	@Test
	public void testBiggestMonthDayToInt() {
		assertThat( BRIDGE.encode( MonthDay.of( 12, 31 ) ) ).isEqualTo( 1231 );
	}

	@Test
	public void testSmallestMonthDayToInt() {
		assertThat( BRIDGE.encode( MonthDay.of( 1, 1 ) ) ).isEqualTo( 101 );
	}

	@Test
	public void testIntToMonthDay() {
		assertThat( BRIDGE.decode( 102 ) ).isEqualTo( MonthDay.of( 1, 2 ) );
	}

	@Test
	public void testIntToBiggestMonthDay() {
		assertThat( BRIDGE.decode( 1231 ) ).isEqualTo( MonthDay.of( 12, 31 ) );
	}

	@Test
	public void testIntToSmallestMonthDay() {
		assertThat( BRIDGE.decode( 101 ) ).isEqualTo( MonthDay.of( 1, 1 ) );
	}
}
