/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.search.bridge.builtin.time.impl.LocalDateTimeBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class LocalDateTimeBridgeTest {

	private static final LocalDateTimeBridge BRIDGE = LocalDateTimeBridge.INSTANCE;

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalDateTime.MAX ) ).isEqualTo( "+9999999991231235959999999999" );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalDateTime.MIN ) ).isEqualTo( "-9999999990101000000000000000" );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		LocalDate date = LocalDate.of( 1, 2, 3 );
		LocalTime time = LocalTime.of( 4, 5, 6, 7 );
		assertThat( BRIDGE.objectToString( LocalDateTime.of( date, time ) ) ).isEqualTo( "+0000000010203040506000000007" );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( "+9999999991231235959999999999" ) ).isEqualTo( LocalDateTime.MAX );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( "-9999999990101000000000000000" ) ).isEqualTo( LocalDateTime.MIN );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		LocalDate date = LocalDate.of( 1, 2, 3 );
		LocalTime time = LocalTime.of( 4, 5, 6, 7 );
		assertThat( BRIDGE.stringToObject( "+0000000010203040506000000007" ) ).isEqualTo( LocalDateTime.of( date, time ) );
	}
}
