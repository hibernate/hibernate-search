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
public interface FuzzyContext extends QueryCustomization<FuzzyContext> {
	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext onField(String field);

	/**
	 * fields / properties the term query is executed on
	 */
	TermMatchingContext onFields(String... fields);

	/**
	 * Threshold above which two terms are considered similar enough.
	 * Value between 0 and 1 (1 excluded)
	 * Defaults to .5
	 *
	 * @deprecated use {@link #withEditDistanceUpTo(int)}
	 */
	@Deprecated
	FuzzyContext withThreshold(float threshold);

	/**
	 * Maximum value of the edit distance. Roughly speaking, the number of changes between two terms to be considered
	 * close enough.
	 * Can be either 1 or 2 (0 would mean no fuzziness).
	 *
	 * Defaults to 2.
	 */
	FuzzyContext withEditDistanceUpTo(int maxEditDistance);

	/**
	 * Size of the prefix ignored by the fuzzyness.
	 * A non zero value is recommended if the index contains a huge amount of distinct terms
	 *
	 * Defaults to 0
	 */
	FuzzyContext withPrefixLength(int prefixLength);
}
