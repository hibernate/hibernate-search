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
