/*
 * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;


/**
 * Use map to delegate to the right JGroupsMasterMessageListener.
 *
 * @author Ales Justin
 */
public class MapMultiJGroupsMasterMessageListener implements MultiJGroupsMasterMessageListener {
    private ConcurrentMap<String, JGroupsMasterMessageListener> listeners = new ConcurrentHashMap<String, JGroupsMasterMessageListener>();

    public void addContext(BuildContext context, Properties properties) {
        JGroupsMasterMessageListener listener = createListener(context, properties);
        IndexManagerHolder managerHolder = context.getAllIndexesManager();
        for (IndexManager manager : managerHolder.getIndexManagers()) {
            listeners.putIfAbsent(manager.getIndexName(), listener);
        }
    }

    protected JGroupsMasterMessageListener createListener(BuildContext context, Properties properties) {
        NodeSelectorStrategyHolder holder = context.requestService(MasterSelectorServiceProvider.class);
        return new JGroupsMasterMessageListener(context, holder);
    }

    public void removeContext(BuildContext context) {
        context.releaseService(MasterSelectorServiceProvider.class);
        IndexManagerHolder managerHolder = context.getAllIndexesManager();
        for (IndexManager manager : managerHolder.getIndexManagers()) {
            listeners.remove(manager.getIndexName());
        }
    }

    public void receive(Message message) {
        final byte[] rawBuffer = message.getRawBuffer();
        final String indexName = MessageSerializationHelper.extractIndexName(rawBuffer);
        JGroupsMasterMessageListener listener = listeners.get(indexName);
        if (listener != null) {
            listener.receive(message, rawBuffer, indexName);
        }
    }

    public void viewAccepted(View new_view) {
        for (JGroupsMasterMessageListener listener : listeners.values()) {
            listener.viewAccepted(new_view);
        }
    }

    public void suspect(Address suspected_mbr) {
    }

    public void block() {
    }

    public void unblock() {
    }

    public void getState(OutputStream output) throws Exception {
    }

    public void setState(InputStream input) throws Exception {
    }
}
