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
public interface RangeMatchingContext extends FieldCustomization<RangeMatchingContext> {
	/**
	 * field / property the term query is executed on
	 */
	RangeMatchingContext andField(String field);

	//TODO what about numeric range query, I guess we can detect it automatically based on the field bridge
	//TODO get info on precisionStepDesc (index time info)
	//FIXME: Is <T> correct or should we specialize to String and Numeric (or all the numeric types?
	<T> FromRangeContext<T> from(T from);

	public interface FromRangeContext<T> {
		RangeTerminationExcludable to(T to);
		FromRangeContext<T> excludeLimit();
	}

	/**
	 * The field value must be below <code>below</code>
	 * You can exclude the value <code>below</code> by calling <code>.excludeLimit()</code>
	 */
	RangeTerminationExcludable below(Object below);

	/**
	 * The field value must be above <code>above</code>
	 * You can exclude the value <code>above</code> by calling <code>.excludeLimit()</code>
	 */
	RangeTerminationExcludable above(Object above);

}
