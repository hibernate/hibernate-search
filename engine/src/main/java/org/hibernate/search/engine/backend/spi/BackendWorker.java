/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.index.spi.IndexManager;

/**
 * @author Yoann Rodiere
 */
public interface BackendWorker {

	// TODO decide on parameters and return type
	void search(Object tenantId, Set<IndexManager<?>> indexManagers);

}
