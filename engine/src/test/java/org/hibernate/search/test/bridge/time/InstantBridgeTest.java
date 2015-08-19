/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Instant;

import org.hibernate.search.bridge.builtin.time.impl.InstantBridge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class InstantBridgeTest {

	private static final InstantBridge BRIDGE = InstantBridge.INSTANCE;

	private static final String MAX = "+" + String.valueOf( Instant.MAX.getEpochSecond() ) + String.valueOf( Instant.MAX.getNano() );
	private static final String MIN = String.valueOf( Instant.MIN.getEpochSecond() ) + "000000000";
	private static final String CUSTOM = "+00000000000012345" + "000000001";

	private static final Instant MAX_INSTANT = Instant.MAX;
	private static final Instant MIN_INSTANT = Instant.MIN;
	private static final Instant CUSTOM_INSTANT = Instant.ofEpochSecond( 12345, 1 );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_INSTANT ) ).isEqualTo( MAX );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MIN_INSTANT ) ).isEqualTo( MIN );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_INSTANT ) ).isEqualTo( CUSTOM );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MAX ) ).isEqualTo( MAX_INSTANT );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( MIN ) ).isEqualTo( MIN_INSTANT );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( CUSTOM ) ).isEqualTo( CUSTOM_INSTANT );
	}
}
