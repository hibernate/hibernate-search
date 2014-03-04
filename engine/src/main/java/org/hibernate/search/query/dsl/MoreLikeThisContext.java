/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface MoreLikeThisContext extends QueryCustomization<MoreLikeThisContext> {

	/**
	 * Exclude the entity used for comparison from the results
	 */
	MoreLikeThisContext excludeEntityUsedForComparison();

	/**
	 * Boost significant terms relative to their scores.
	 * Amplified by the boost factor (1 is the recommended default to start with).
	 *
	 * Unless activated, terms are not boosted by their individual frequency.
	 * When activated, significant terms will have their influence increased more than by default.
	 */
	MoreLikeThisContext favorSignificantTermsWithFactor(float factor);

	/**
	 * Match the content using "all" of the indexed fields of the entity.
	 *
	 * More precisely, only fields storing term vectors or physically stored will be compared.
	 * Fields storing the id and the class type are ignored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 */
	MoreLikeThisTerminalMatchingContext comparingAllFields();

	/**
	 * Match the content using the selected fields of the entity.
	 *
	 * An exception is thrown if the fields are neither storing the term vectors nor physically stored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 */
	MoreLikeThisOpenedMatchingContext comparingFields(String... fieldNames);

	/**
	 * Match the content using the selected field of the entity.
	 *
	 * An exception is thrown if the field are neither storing the term vectors nor physically stored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 */
	MoreLikeThisOpenedMatchingContext comparingField(String fieldName);

}
