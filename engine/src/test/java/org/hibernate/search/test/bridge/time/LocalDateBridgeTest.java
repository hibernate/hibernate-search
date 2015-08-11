/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalDate;

import org.hibernate.search.bridge.builtin.time.impl.LocalDateNumericBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class LocalDateBridgeTest {

	private static final LocalDateNumericBridge BRIDGE = LocalDateNumericBridge.INSTANCE;

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testLocaDateToLong() {
		LocalDate date = LocalDate.of( 2012, 10, 7 );
		assertThat( BRIDGE.encode( date ) ).isEqualTo( 20121007L );
	}

	@Test
	public void testBiggestLocaDateToLong() {
		assertThat( BRIDGE.encode( LocalDate.MAX ) ).isEqualTo( 999999999_12_31L );
	}

	@Test
	public void testSmallestLocaDateToLong() {
		assertThat( BRIDGE.encode( LocalDate.MIN ) ).isEqualTo( -9999999990101L );
	}

	@Test
	public void testLongToLocaDate() {
		LocalDate date = LocalDate.of( 2012, 10, 7 );
		assertThat( BRIDGE.decode( 20121007L ) ).isEqualTo( date );
	}

	@Test
	public void testLongToBiggestLocalDate() {
		assertThat( BRIDGE.decode( 9999999991231L ) ).isEqualTo( LocalDate.MAX );
	}

	@Test
	public void testLongToSmallestLocalDate() {
		assertThat( BRIDGE.decode( -9999999990101L ) ).isEqualTo( LocalDate.MIN );
	}
}
