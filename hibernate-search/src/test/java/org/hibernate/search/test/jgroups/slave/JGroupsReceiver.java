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

import org.hibernate.search.backend.LuceneWork;

/**
 * @author Lukasz Moren
 */
public class JGroupsReceiver extends ReceiverAdapter {

	public static int queues;
	public static int works;

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void receive(Message message) {

		List<LuceneWork> queue;
		try {
			queue = ( List<LuceneWork> ) message.getObject();
		}

		catch ( ClassCastException e ) {
			return;
		}
		queues++;
		works += queue.size();
	}
}
