/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.jgroups.impl;

import java.io.InputStream;
import java.io.OutputStream;

import org.jgroups.Message;
import org.jgroups.MessageListener;

/**
 * Wrap deserialization into explict CL TCCL.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Sanne Grinovero
 */
class ClassloaderMessageListener implements MessageListener {

	private final MessageListener delegate;
	private final ClassLoader cl;

	ClassloaderMessageListener(MessageListener delegate, ClassLoader cl) {
		this.delegate = delegate;
		this.cl = cl;
	}

	@Override
	public void receive(final Message msg) {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader previous = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader( cl );
		try {
			delegate.receive( msg );
		}
		finally {
			currentThread.setContextClassLoader( previous );
		}
	}

	@Override
	public void getState(final OutputStream output) throws Exception {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader previous = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader( cl );
		try {
			delegate.getState( output );
		}
		finally {
			currentThread.setContextClassLoader( previous );
		}
	}

	@Override
	public void setState(final InputStream input) throws Exception {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader previous = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader( cl );
		try {
			delegate.setState( input );
		}
		finally {
			currentThread.setContextClassLoader( previous );
		}
	}
}
