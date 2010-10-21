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

import org.apache.lucene.search.Query;

/**
 * Represents a boolean query that can contains one or more elements to join
 *
 * @author Emmanuel Bernard
 */
public interface BooleanJunction<T extends BooleanJunction> extends QueryCustomization<T>, Termination {
	/**
	 * The boolean query results should match the subquery
	 */
	BooleanJunction should(Query query);

	/**
	 * The boolean query results must (or must not) match the subquery
	 * Call the .not() method to ensure results of the boolean query do NOT match the subquery.
	 */
	MustJunction must(Query query);
}
