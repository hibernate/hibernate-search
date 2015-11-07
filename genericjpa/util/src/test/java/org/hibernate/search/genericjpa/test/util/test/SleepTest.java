/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.util.test;

import org.hibernate.search.genericjpa.util.Sleep;

import org.junit.Test;

import static org.junit.Assert.fail;

public class SleepTest {

	@Test
	public void test() throws InterruptedException {
		Sleep.sleep(
				1000, () -> {
					return true;
				}
		);
		boolean val[] = {false};
		Sleep.sleep(
				1000, () -> {
					if ( val[0] ) {
						return true;
					}
					val[0] = true;
					return false;
				}
		);
		try {
			Sleep.sleep(
					1000, () -> {
						return false;
					}
			);
			fail( "timeout expected!" );
		}
		catch (RuntimeException e) {
		}
	}

}
