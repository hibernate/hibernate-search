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
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

/**
 * We use the MessageDispatcher instead of the JChannel to be able to use blocking
 * operations (optionally) without having to rely on the RSVP protocol
 * being configured on the stack.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
final class DispatcherMessageSender implements MessageSender {

	private static final Log log = LoggerFactory.make();

	private final MessageDispatcher dispatcher;
	private final Channel channel;

	DispatcherMessageSender(final MessageDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.channel = dispatcher.getChannel();
	}

	@Override
	public Address getAddress() {
		return channel.getAddress();
	}

	@Override
	public View getView() {
		return channel.getView();
	}

	@Override
	public void stop() {
		dispatcher.stop();
	}

	@Override
	public void send(final Message message, final boolean synchronous, final long timeout) throws Exception {
		final RequestOptions options = synchronous ? RequestOptions.SYNC() : RequestOptions.ASYNC();
		options.setExclusionList( dispatcher.getChannel().getAddress() );
		options.setTimeout( timeout );
		RspList<Object> rspList = dispatcher.castMessage( null, message, options );
		//JGroups won't throw these automatically as it would with a JChannel usage,
		//so we provide the same semantics by throwing the JGroups specific exceptions
		//as appropriate
		if ( synchronous ) {
			for ( Rsp rsp : rspList.values() ) {
				if ( !rsp.wasReceived() ) {
					if ( rsp.wasSuspected() ) {
						throw log.jgroupsSuspectingPeer( rsp.getSender() );
					}
					else {
						throw log.jgroupsRpcTimeout( rsp.getSender() );
					}
				}
				else {
					if ( rsp.hasException() ) {
						throw log.jgroupsRemoteException( rsp.getSender(), rsp.getException(), rsp.getException() );
					}
				}
			}
		}
	}

}
