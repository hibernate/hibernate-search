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
	 * Use a JPQL/HQL conditional expression to limit the entities to be re-indexed.
	 * <p>
	 * For instance a valid expression could be the following:
	 * <pre>
	 *     manager.level &lt; 2
	 * </pre>
	 * ... to filter instances that have a manager whose level is strictly less than 2.
	 * <p>
	 * Parameters can be used, so assuming the parameter "max" is defined
	 * in the {@link MassIndexerReindexParameterStep#param(String, Object) next step},
	 * this is valid as well:
	 * <pre>
	 *     manager.level &lt; :max
	 * </pre>
	 * ... to filter instances that have a manager whose level is strictly less than {@code :max}.
	 *
	 * @param conditionalExpression A JPQL/HQL query text which expresses the condition to apply
	 * @return A new step to define optional parameters for the JPQL/HQL conditional expression or other expressions.
	 */
	MassIndexerReindexParameterStep reindexOnly(String conditionalExpression);

}
