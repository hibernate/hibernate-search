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
import org.hibernate.search.exception.SearchException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
public class InstantBridgeTest {

	private static final InstantBridge BRIDGE = InstantBridge.INSTANCE;

	private static final Instant MAX_INSTANT = Instant.ofEpochMilli( Long.MAX_VALUE );
	private static final Instant CUSTOM_INSTANT = Instant.ofEpochSecond( 12345, 1 );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( MAX_INSTANT ) ).isEqualTo( String.valueOf( MAX_INSTANT.toEpochMilli() ) );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( CUSTOM_INSTANT ) ).isEqualTo( String.valueOf( CUSTOM_INSTANT.toEpochMilli() ) );
	}

	@Test
	public void testTooLargeValueException() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000297" );

		BRIDGE.objectToString( Instant.MAX );
	}
}
