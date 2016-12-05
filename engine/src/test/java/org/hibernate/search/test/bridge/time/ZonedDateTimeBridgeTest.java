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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.search.bridge.builtin.time.impl.ZonedDateTimeBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class ZonedDateTimeBridgeTest {

	private static final ZonedDateTimeBridge BRIDGE = ZonedDateTimeBridge.INSTANCE;

	private static final String MAX = "+9999999991231235959999999999+18:00";
	private static final String MIN = "-9999999990101000000000000000-18:00";
	private static final String CUSTOM_LEGACY_FORMAT = "+0000020010203040506000000007Europe/Paris";
	private static final String CUSTOM = "+0000020010203040506000000007+01:00Europe/Paris";
	private static final String BC_CUSTOM_LEGACY_FORMAT = "-0000000010203040506000000007Europe/Paris";
	// The offset for Europe/Paris was 9 minutes 21 seconds until the beginning of the XXth century
	private static final String BC_CUSTOM = "-0000000010203040506000000007+00:09:21Europe/Paris";

	private static final ZonedDateTime MAX_VALUE = ZonedDateTime.of( LocalDateTime.MAX, ZoneOffset.MAX );
	private static final ZonedDateTime MIN_VALUE = ZonedDateTime.of( LocalDateTime.MIN, ZoneOffset.MIN );
	private static final ZonedDateTime CUSTOM_VALUE = ZonedDateTime.of( LocalDate.of( 2001, 2, 3 ), LocalTime.of( 4, 5, 6, 7 ), ZoneId.of( "Europe/Paris" ) );
	private static final ZonedDateTime BC_CUSTOM_VALUE = ZonedDateTime.of( LocalDate.of( -1, 2, 3 ), LocalTime.of( 4, 5, 6, 7 ), ZoneId.of( "Europe/Paris" ) );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_VALUE ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_VALUE ) ).isEqualTo( MIN );
	}

	@Test
	public void testCustomObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_VALUE ) ).isEqualTo( CUSTOM );
	}

	@Test
	public void testBcCustomObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( BC_CUSTOM_VALUE ) ).isEqualTo( BC_CUSTOM );
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
	public void testCustomStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CUSTOM ) ).isEqualTo( CUSTOM_VALUE );
	}

	@Test
	public void testCustomLegacyStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CUSTOM_LEGACY_FORMAT ) ).isEqualTo( CUSTOM_VALUE );
	}

	@Test
	public void testBcCustomStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( BC_CUSTOM ) ).isEqualTo( BC_CUSTOM_VALUE );
	}

	@Test
	public void testBcCustomLegacyStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( BC_CUSTOM_LEGACY_FORMAT ) ).isEqualTo( BC_CUSTOM_VALUE );
	}
}
