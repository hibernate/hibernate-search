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
 * Wrap receive into explict CL TCCL.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ClassloaderMessageListener implements MessageListener {
    private MessageListener delegate;
    private ClassLoader cl;

    ClassloaderMessageListener(MessageListener delegate, ClassLoader cl) {
        this.delegate = delegate;
        this.cl = cl;
    }

    public void receive(Message msg) {
        final ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            delegate.receive(msg);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    public void getState(OutputStream output) throws Exception {
        delegate.getState(output);
    }

    public void setState(InputStream input) throws Exception {
        delegate.setState(input);
    }
}
