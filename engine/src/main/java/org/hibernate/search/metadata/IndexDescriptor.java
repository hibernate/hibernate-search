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
 * @author Hardy Ferentschik
 */
// TODO under construction
public interface IndexDescriptor {
	/**
	 * @return the name of the Lucene index (unless it is sharded in which case {@link #getShardNames()} should be used.
	 */
	String indexName();

	/**
	 * @return {@code true} is this index is sharded, {@code false} otherwise
	 */
	boolean isSharded();

	/**
	 * @return the set of index names for a sharded index. In case the index is not sharded the set will just contain the
	 *         single index name. See {@link #indexName()}.
	 */
	Set<String> getShardNames();
}


