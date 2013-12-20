/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
