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
import java.time.ZoneOffset;
import java.time.OffsetDateTime;

import org.hibernate.search.bridge.builtin.time.impl.OffsetDateTimeBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class OffsetDateTimeBridgeTest {

	private static final OffsetDateTimeBridge BRIDGE = OffsetDateTimeBridge.INSTANCE;

	private static final String MAX = "+9999999991231235959999999999+18:00";
	private static final String MIN = "-9999999990101000000000000000-18:00";
	private static final String CUSTOM = "-0000000010203040506000000007+08:00";

	private static final OffsetDateTime MAX_DATE_TIME = OffsetDateTime.of( LocalDateTime.MAX, ZoneOffset.MAX );
	private static final OffsetDateTime MIN_DATE_TIME = OffsetDateTime.of( LocalDateTime.MIN, ZoneOffset.MIN );
	private static final OffsetDateTime CUSTOM_DATE_TIME = OffsetDateTime.of( LocalDate.of( -1, 2, 3 ), LocalTime.of( 4, 5, 6, 7 ), ZoneOffset.ofHours( 8 ) );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_DATE_TIME ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_DATE_TIME ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_DATE_TIME ) ).isEqualTo( CUSTOM );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( MAX_DATE_TIME );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( MIN_DATE_TIME );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CUSTOM ) ).isEqualTo( CUSTOM_DATE_TIME );
	}
}
