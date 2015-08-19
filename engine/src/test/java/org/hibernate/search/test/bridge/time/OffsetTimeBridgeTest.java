/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.hibernate.search.bridge.builtin.time.impl.OffsetTimeBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class OffsetTimeBridgeTest {

	private static final OffsetTimeBridge BRIDGE = OffsetTimeBridge.INSTANCE;

	private static final String MAX = "235959999999999+18:00";
	private static final String MIN = "000000000000000-18:00";
	private static final String CUSTOM = "040506000000007+08:00";

	private static final OffsetTime MAX_UTC = OffsetTime.of( LocalTime.MAX, ZoneOffset.MAX );
	private static final OffsetTime MIN_UTC = OffsetTime.of( LocalTime.MIN, ZoneOffset.MIN );
	private static final OffsetTime CUSTOM_UTC = OffsetTime.of( LocalTime.of( 4, 5, 6, 7 ), ZoneOffset.ofHours( 8 ) );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_UTC ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_UTC ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_UTC ) ).isEqualTo( CUSTOM );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( MAX_UTC );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( MIN_UTC );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CUSTOM ) ).isEqualTo( CUSTOM_UTC );
	}
}
