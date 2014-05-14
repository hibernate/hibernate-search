/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.jgroups.impl;

import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.blocks.RequestHandler;

/**
 * Delegate request to listener.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class MessageListenerToRequestHandlerAdapter implements RequestHandler {

	private final MessageListener delegate;

	public MessageListenerToRequestHandlerAdapter(final MessageListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object handle(final Message msg) throws Exception {
		delegate.receive( msg );
		return null;
	}
}
