/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalTime;

import org.hibernate.search.bridge.builtin.time.impl.LocalTimeBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class LocalTimeBridgeTest {

	private static final LocalTimeBridge BRIDGE = LocalTimeBridge.INSTANCE;
	private static final String MAX = "235959999999999";
	private static final String MIN = "000000000000000";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalTime.MAX ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalTime.MIN ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalTime.of( 4, 5, 6, 7 ) ) ).isEqualTo( "040506000000007" );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( LocalTime.MAX );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( LocalTime.MIN );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( "040506000000007" ) ).isEqualTo( LocalTime.of( 4, 5, 6, 7 ) );
	}
}
