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

package org.hibernate.search.backend.impl.jgroups;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jgroups.Channel;
import org.jgroups.Message;

/**
 * Channel message sender.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
final class ChannelMessageSender extends AbstractMessageSender {

	private static final Log log = LoggerFactory.make();

	private final boolean channelIsManaged;
	private final String clusterName;

	ChannelMessageSender(Channel channel, boolean channelIsManaged, String clusterName) {
		super( channel );
		this.channelIsManaged = channelIsManaged;
		this.clusterName = clusterName;
	}

	public void start() {
		if ( channel != null && channelIsManaged ) {
			try {
				channel.connect( clusterName );
			}
			catch ( Exception e ) {
				throw log.unableConnectingToJGroupsCluster( clusterName, e );
			}
		}
	}

	public void stop() {
		if ( channel != null && channel.isOpen() && channelIsManaged ) {
			log.jGroupsDisconnectingAndClosingChannel( clusterName );
			channel.disconnect();
			channel.close();
		}
	}

	public void send(final Message message) throws Exception {
		channel.send( message );
	}
}
