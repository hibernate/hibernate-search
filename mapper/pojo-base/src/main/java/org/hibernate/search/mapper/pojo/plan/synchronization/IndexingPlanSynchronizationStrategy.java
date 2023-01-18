/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.plan.synchronization;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Determines how the thread will block upon committing a transaction
 * where indexed entities were modified.
 *
 * {@code SearchSession#indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy)}
 */
@Incubating
public interface IndexingPlanSynchronizationStrategy<E> {

	void apply(IndexingPlanSynchronizationStrategyConfigurationContext<E> context);

}
