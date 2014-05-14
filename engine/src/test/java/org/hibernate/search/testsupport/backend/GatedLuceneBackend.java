/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.backend;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;

/**
 * A backend to use as test helper able to "switch off" the backend, to have it stop
 * processing operations. When the gate is closed, all operations are lost.
 *
 * Especially useful to test functionality while the index is updated asynchronously.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class GatedLuceneBackend extends LeakingLuceneBackend {

	public static final AtomicBoolean open = new AtomicBoolean( true );

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( open.get() ) {
			super.applyWork( workList, monitor );
		}
	}

}
