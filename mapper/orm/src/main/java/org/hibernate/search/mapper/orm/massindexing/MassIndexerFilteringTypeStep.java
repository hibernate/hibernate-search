/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

import java.util.function.Consumer;

import org.hibernate.query.Query;

/**
 * This step allows to define a filter on entities of a given type that has to be re-indexed
 */
public interface MassIndexerFilteringTypeStep {

	/**
	 * Use a JPQL/HQL conditional expression to limit the entities to be re-indexed.
	 * <p>
	 * If the JPQL/HQL conditional expression contains parameters use {@link #reindexOnly(String, Consumer)}.
	 * <p>
	 * The letter {@code e} is supposed to be used here as query alias.
	 * For instance a valid expression could be the following:
	 * <pre>
	 *     <strong>e</strong>.manager.level &lt; 2
	 * </pre>
	 * To filter instances that have a manager whose level is strictly less than 2.
	 *
	 * @param conditionalExpression A JPQL/HQL query text which express the condition to apply
	 * @return {@code this}, for method chaining
	 */
	MassIndexerFilteringTypeStep reindexOnly(String conditionalExpression);

	/**
	 * Use a JPQL/HQL conditional expression to limit the entities to be re-indexed.
	 * <p>
	 * The letter {@code e} is supposed to be used here as query alias.
	 * <p>
	 * The letter {@code e} is supposed to be used here as query alias.
	 * For instance a valid expression could be the following:
	 * <pre>
	 *     <strong>e</strong>.manager.level &lt; 2
	 * </pre>
	 * To filter instances that have a manager whose level is strictly less than 2.
	 *
	 * @param conditionalExpression A JPQL query text which express the condition to apply
	 * @param queryConsumer A {@link Query} consumer that allows to define parameters for the given JPQL query text
	 * @return {@code this}, for method chaining
	 */
	MassIndexerFilteringTypeStep reindexOnly(String conditionalExpression, Consumer<Query<?>> queryConsumer);

}
