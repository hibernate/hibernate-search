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

package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;

/**
 * Abstract away message submission.
 *
 * Even though an internal contract we use the service mechanism to get life cycle management.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface MessageSenderService extends Service, Startable, Stoppable {
	/**
	 * Send message.
	 *
	 * @param message the JGroups message
	 * @param synchronous set to true if we need to block until an ACK is received
	 * @param messageTimeout in milliseconds
	 * @throws java.lang.Exception for any error
	 */
	void send(Message message, boolean synchronous, long messageTimeout) throws Exception;

	/**
	 * Get sender's address.
	 *
	 * @return the sender's address
	 */
	Address getAddress();

	/**
	 * Get current view.
	 *
	 * @return the current cluster view
	 */
	View getView();
}
