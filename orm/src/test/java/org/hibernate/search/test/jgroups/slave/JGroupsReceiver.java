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
package org.hibernate.search.test.jgroups.slave;

import java.util.List;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jgroups.MessageSerializationHelper;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * @author Lukasz Moren
 */
public class JGroupsReceiver extends ReceiverAdapter {

	public static volatile int queues;
	public static volatile int works;
	private SearchFactoryImplementor searchFactory;

	public JGroupsReceiver(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
	}

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@Override
	public void receive(Message message) {
		try {
			final byte[] rawBuffer = message.getRawBuffer();
			final int messageOffset = message.getOffset();
			final int bufferLength = message.getLength();
			String indexName = MessageSerializationHelper.extractIndexName( messageOffset, rawBuffer );
			byte[] serializedQueue = MessageSerializationHelper.extractSerializedQueue( messageOffset, bufferLength, rawBuffer );
			IndexManager indexManager = searchFactory.getIndexManagerHolder().getIndexManager( indexName );
			List<LuceneWork> queue = indexManager.getSerializer().toLuceneWorks( serializedQueue );
			queues++;
			works += queue.size();
		}
		catch (ClassCastException e) {
			throw new SearchException( e );
		}
	}
}
