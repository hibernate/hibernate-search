/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.engine.service.spi.Service;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.util.Buffer;

/**
 * Abstract away message submission.
 *
 * Even though an internal contract we use the service mechanism to get life cycle management.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface MessageSenderService extends Service {

	/**
	 * Send message.
	 *
	 * @param data the serialized message to transmit
	 * @param synchronous set to true if we need to block until an ACK is received
	 * @param messageTimeout in milliseconds
	 * @throws java.lang.Exception for any error
	 */
	void send(Buffer data, boolean synchronous, long messageTimeout) throws Exception;

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
