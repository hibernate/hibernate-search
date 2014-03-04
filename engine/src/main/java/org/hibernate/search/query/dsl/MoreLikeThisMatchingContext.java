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
 * Super interface offering the way to provide the matching content
 * as well as customize the preceding field.
 *
 * Sub interfaces are expected to clarify whether additional fields can be set.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface MoreLikeThisMatchingContext {

	/**
	 * Find other entities looking like the entity with the given id.
	 * Only the selected fields will be used for comparison.
	 */
	MoreLikeThisTermination toEntityWithId(Object id);

	/*
	 * Find other entities looking like the entity provided.
	 * Only the selected fields will be used for comparison.
	 * If the provided entity is already indexed, the index data is used.
	 * Otherwise, we use the value of each property in the instance passed.
	 */
	//TODO genericize it
	MoreLikeThisToEntityContentAndTermination toEntity(Object entity);

}
