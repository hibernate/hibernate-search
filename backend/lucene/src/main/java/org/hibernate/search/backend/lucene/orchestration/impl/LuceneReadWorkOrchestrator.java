/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;


public interface LuceneReadWorkOrchestrator {

	<T> T submit(Set<String> indexNames, Set<? extends ReadIndexManagerContext> indexManagerContexts,
			Set<String> routingKeys, ReadWork<T> work);

}
