/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;

/**
 * An orchestrator that executes works synchronously in the current thread.
 * <p>
 * For now this implementation is very simple,
 * but we might one day need to execute queries asynchronously,
 * in which case thing will get slightly more complex.
 */
public interface LuceneSyncWorkOrchestrator {

	default <T> T submit(Set<String> indexNames, Collection<? extends ReadIndexManagerContext> indexManagerContexts,
			Set<String> routingKeys, ReadWork<T> work) {
		return submit( indexNames, indexManagerContexts, routingKeys, work, null );
	}

	<T> T submit(Set<String> indexNames, Collection<? extends ReadIndexManagerContext> indexManagerContexts,
			Set<String> routingKeys, ReadWork<T> work, HibernateSearchMultiReader indexReader);

}
