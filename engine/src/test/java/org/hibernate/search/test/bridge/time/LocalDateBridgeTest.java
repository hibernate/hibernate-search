/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalDate;

import org.hibernate.search.bridge.builtin.time.impl.LocalDateBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class LocalDateBridgeTest {

	private static final LocalDateBridge BRIDGE = LocalDateBridge.INSTANCE;

	@Test
	public void testMaxObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalDate.MAX ) ).isEqualTo( "+9999999991231" );
	}

	@Test
	public void testMinObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalDate.MIN ) ).isEqualTo( "-9999999990101" );
	}

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( LocalDate.of( 1, 2, 3 ) ) ).isEqualTo( "+0000000010203" );
	}

	@Test
	public void testMaxStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( "+9999999991231" ) ).isEqualTo( LocalDate.MAX );
	}

	@Test
	public void testMinStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( "-9999999990101" ) ).isEqualTo( LocalDate.MIN );
	}

	@Test
	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( "+0000000010203" ) ).isEqualTo( LocalDate.of( 1, 2, 3 ) );
	}
}
