/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
