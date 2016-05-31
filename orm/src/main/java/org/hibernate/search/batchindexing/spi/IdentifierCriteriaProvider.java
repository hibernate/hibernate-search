/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.spi;

import org.hibernate.criterion.Criterion;

/**
 * Contract that allows external sources to influence the production of identifiers during
 * the operations of a {@link org.hibernate.search.MassIndexer} execution.
 *
 * If multiple criteria are required to produce the necessary number of identifiers for the
 * mass indexing operation, you must wrap those in the appropriate conjunction/disjunction
 * criterion to provide a single {@link Criterion} instance.
 *
 * @author Chris Cranford
 */
public interface IdentifierCriteriaProvider {
	/**
	 * Get the criteria that should be applied to influence the production of identifiers.
	 *
	 * @param indexedType the entity class to produce criteria for.
	 * @return the additional {@link Criterion} to apply.
	 */
	Criterion getIdentifierCriteria(Class<?> indexedType);
}
