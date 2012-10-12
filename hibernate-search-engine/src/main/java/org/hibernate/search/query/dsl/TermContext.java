/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface TermContext extends QueryCustomization<TermContext> {
	/**
	 * @param field The field name the term query is executed on
	 *
	 * @return {@code TermMatchingContext} to continue the term query
	 */
	TermMatchingContext onField(String field);

	/**
	 * @param field The field names the term query is executed on. The underlying properties for the specified
	 * fields need to be of the same type. For example, it is not possible to use this method with a mixture of
	 * string and date properties. In the mixed case an alternative is to build multiple term queries and combine them
	 * via {@link org.hibernate.search.query.dsl.QueryBuilder#bool()}
	 * @return {@code TermMatchingContext} to continue the term query
	 */
	TermMatchingContext onFields(String... field);

	/**
	 * Use a fuzzy search approximation (aka edit distance)
	 *
	 * @return {@code FuzzyContext} to continue the fuzzy query
	 */
	FuzzyContext fuzzy();

	/**
	 * Treat the query as a wildcard query which means:
	 * <ul>
	 * <li> '?' represents any single character</li>
	 * <li> '*' represents any character sequence </li>
	 * </ul>
	 * For faster results, it is recommended that the query text does not
	 * start with '?' or '*'.
	 *
	 * @return {@code WildcardContext} to continue the wildcard query
	 */
	WildcardContext wildcard();
}
