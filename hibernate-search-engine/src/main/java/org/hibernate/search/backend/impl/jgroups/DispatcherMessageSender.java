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

import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;

/**
 * Channel message sender.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class DispatcherMessageSender extends AbstractMessageSender {

	private final MessageDispatcher dispatcher;

	DispatcherMessageSender(final MessageDispatcher dispatcher) {
		super( dispatcher.getChannel() );
		this.dispatcher = dispatcher;
	}

	public void stop() {
		dispatcher.stop();
	}

	public void send(final Message message) throws Exception {
		dispatcher.castMessage( null, message, RequestOptions.ASYNC() );
	}

	@Override
	public void start() {
	}
}
