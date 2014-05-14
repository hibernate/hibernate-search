/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.jgroups.common;

import org.jgroups.JChannel;

/**
 * Test master / slave injected mux supported channel.
 *
 * @author Ales Justin
 */
public class MSMuxChannelTest extends MuxChannelTest {

	@Override
	protected JChannel[] createChannels() throws Exception {
		JChannel[] channels = new JChannel[2];
		channels[0] = createChannel();
		channels[1] = createChannel();
		return channels;
	}

	@Override
	protected String getMasterBackend() {
		return "jgroupsMaster";
	}

	@Override
	protected String getSlaveBackend() {
		return "jgroupsSlave";
	}
}
