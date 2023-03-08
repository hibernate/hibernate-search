/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.schema.management.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;

public interface SchemaManagementIndexManagerContext {

	Collection<LuceneParallelWorkOrchestrator> allManagementOrchestrators();

	Optional<String> backendName();
}
