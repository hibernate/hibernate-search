/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.ZoneId;

import org.hibernate.search.bridge.builtin.time.impl.ZoneIdBridge;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class ZoneIdBridgeTest {

	private static final ZoneIdBridge BRIDGE = ZoneIdBridge.INSTANCE;

	private static final ZoneId VALUE_EXPECTED = ZoneId.of( "UTC" );
	private static final String STRING_EXPECTED = "UTC";

	@Test
	public void testPaddingObjectToString() throws Exception {
		assertThat( BRIDGE.objectToString( VALUE_EXPECTED ) ).isEqualTo( STRING_EXPECTED );
	}

	public void testPaddingStringToObject() throws Exception {
		assertThat( BRIDGE.stringToObject( STRING_EXPECTED ) ).isEqualTo( VALUE_EXPECTED );
	}
}
