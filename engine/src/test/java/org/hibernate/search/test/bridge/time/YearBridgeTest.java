/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Year;

import org.hibernate.search.bridge.builtin.time.impl.YearNumericBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class YearBridgeTest {

	private static final YearNumericBridge BRIDGE = YearNumericBridge.INSTANCE;

	@Test
	public void testYearToInt() {
		Year year = Year.of( 1 );
		assertThat( BRIDGE.encode( year ) ).isEqualTo( 1 );
	}

	@Test
	public void testBiggestYearToInt() {
		assertThat( BRIDGE.encode( Year.of( Year.MAX_VALUE ) ) ).isEqualTo( 999_999_999 );
	}

	@Test
	public void testSmallestYearToInt() {
		assertThat( BRIDGE.encode( Year.of( Year.MIN_VALUE ) ) ).isEqualTo( -999_999_999 );
	}

	@Test
	public void testIntToYear() {
		assertThat( BRIDGE.decode( 1 ) ).isEqualTo( Year.of( 1 ) );
	}

	@Test
	public void testIntToBiggestYear() {
		assertThat( BRIDGE.decode( 999_999_999 ) ).isEqualTo( Year.of( Year.MAX_VALUE ) );
	}

	@Test
	public void testIntToSmallestYear() {
		assertThat( BRIDGE.decode( -999_999_999 ) ).isEqualTo( Year.of( Year.MIN_VALUE ) );
	}
}
