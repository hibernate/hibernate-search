/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.metadata;

import java.util.Set;

/**
 * Top level descriptor of the metadata API. Giving access to the indexing information for a single entity.
 *
 * @author Hardy Ferentschik
 */
public interface IndexedTypeDescriptor {
	/**
	 * @return {@code true} if the entity for this descriptor is indexed, {@code false} otherwise
	 */
	boolean isIndexed();

	/**
	 * @return the class boost value, 1 being the default.
	 */
	float getClassBoost();

	/**
	 * @return an {@code IndexDescriptor} instance describing Lucene index information
	 */
	IndexDescriptor getIndexDescriptor();

	/**
	 * @return a set of {@code FieldDescriptor}s for the indexed fields of the entity.
	 */
	// TODO does this include the id field descriptor or should that be a separate descriptor?
	// TODO should OBJECT_CLASS be considered?
	Set<FieldDescriptor> getIndexedFields();

	/**
	 * Retrieves the field descriptor for a given field name.
	 *
	 * @param fieldName the field name for which to return descriptor. Cannot be {@code null}
	 *
	 * @return the field descriptor for the specified field name. {@code null} is returned in case a field with the specified name does not exist
	 *
	 * @throws IllegalArgumentException in case {@code fieldName} is {@code null}
	 */
	FieldDescriptor getIndexedField(String fieldName);
}
