/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.osgi;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the current system is able to run tests on Apache Karaf via Pax Exam.
 * If the current hostname does not resolve to 127.0.0.1, these tests will
 * hang and not provide a good error message.
 * This test is put in place to have a better error message when launching the build
 * on a box with exotic network configurations.
 */
public class SystemTest {

	@Test
	public void testLocalhostResolution() throws UnknownHostException {
		String hostname = InetAddress.getLocalHost().getHostName();
		String hostAddress = InetAddress.getLocalHost().getHostAddress();
		Assert.assertEquals(
				"local machine name '" + hostname + "' does not resolve to '127.0.0.1'. Fix your network configuration to be able to run the OSGi integration tests.",
				"127.0.0.1", hostAddress );
	}

}
