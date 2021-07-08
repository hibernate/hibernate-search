/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

/**
 * A step to define optional parameters for the JPQL/HQL conditional expression or other new expressions.
 */
public interface MassIndexerReindexParameterStep extends MassIndexerFilteringTypeStep {

	/**
	 * Bind a new parameter value for a given parameter name.
	 *
	 * @param name The parameter name
	 * @param value The parameter value
	 * @return {@code this}, to define other parameters or new expressions
	 */
	MassIndexerReindexParameterStep param(String name, Object value);

}
