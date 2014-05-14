/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.jgroups.common;

import org.jgroups.JChannel;

/**
 * Test auto injected mux supported channel.
 *
 * @author Ales Justin
 */
public class AutoMuxChannelTest extends MuxChannelTest {

	@Override
	protected JChannel[] createChannels() throws Exception {
		JChannel[] channels = new JChannel[2];
		channels[1] = createChannel(); // order matters in AutoNodeSelector -- 1 ~ "slave"
		channels[0] = createChannel();
		return channels;
	}

	@Override
	protected String getMasterBackend() {
		return "jgroups";
	}

	@Override
	protected String getSlaveBackend() {
		return "jgroups";
	}
}
