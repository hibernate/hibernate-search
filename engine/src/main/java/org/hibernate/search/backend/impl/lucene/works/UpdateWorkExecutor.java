/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class UpdateWorkExecutor implements LuceneWorkExecutor {

	private final DeleteWorkExecutor deleteDelegate;
	private final AddWorkExecutor addDelegate;

	UpdateWorkExecutor(DeleteWorkExecutor deleteDelegate, AddWorkExecutor addDelegate) {
		this.deleteDelegate = deleteDelegate;
		this.addDelegate = addDelegate;
	}

	@Override
	public void performWork(final LuceneWork work, IndexWriterDelegate delegate, final IndexingMonitor monitor) {
		// This is the slowest implementation, needing to remove and then add to the index;
		// see also org.hibernate.search.backend.impl.lucene.works.UpdateExtWorkDelegate
		this.deleteDelegate.performWork( work, delegate, monitor );
		this.addDelegate.performWork( work, delegate, monitor );
	}

}
