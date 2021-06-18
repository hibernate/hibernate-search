/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

/**
 * This step allows to define a filter on entities of a given type that has to be re-indexed
 */
public interface MassIndexerFilteringTypeStep {

	/**
	 * Use a JPQL conditional expression to limit the entities to be re-indexed
	 *
	 * @param jpqlConditionalExpression The condition to apply
	 * @return The mass indexer updated with the new filter
	 */
	MassIndexer reindexOnly(String jpqlConditionalExpression);

}
