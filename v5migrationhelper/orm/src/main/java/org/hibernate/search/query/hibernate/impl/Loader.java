/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * Interface defining a set of operations in order to load entities which matched a query. Depending on the type of
 * indexed entities and the type of query different strategies can be used.
 *
 *
 * @author Emmanuel Bernard
 */
public interface Loader {
	void init(
			SessionImplementor session,
			ExtendedSearchIntegrator extendedIntegrator,
			ObjectInitializer objectInitializer,
			TimeoutManager timeoutManager);

	Object load(EntityInfo entityInfo);

	Object loadWithoutTiming(EntityInfo entityInfo);

	List load(List<EntityInfo> entityInfos);

	boolean isSizeSafe();
}
