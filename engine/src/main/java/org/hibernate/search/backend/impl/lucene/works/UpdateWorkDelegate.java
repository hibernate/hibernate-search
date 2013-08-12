/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class UpdateWorkDelegate implements LuceneWorkDelegate {

	private final DeleteWorkDelegate deleteDelegate;
	private final AddWorkDelegate addDelegate;

	UpdateWorkDelegate(DeleteWorkDelegate deleteDelegate, AddWorkDelegate addDelegate) {
		this.deleteDelegate = deleteDelegate;
		this.addDelegate = addDelegate;
	}

	@Override
	public void performWork(final LuceneWork work, final IndexWriter writer, final IndexingMonitor monitor) {
		// This is the slowest implementation, needing to remove and then add to the index;
		// see also org.hibernate.search.backend.impl.lucene.works.UpdateExtWorkDelegate
		this.deleteDelegate.performWork( work, writer, monitor );
		this.addDelegate.performWork( work, writer, monitor );
	}

}
