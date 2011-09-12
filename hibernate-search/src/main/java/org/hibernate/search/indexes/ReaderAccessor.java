/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes;

import org.apache.lucene.index.IndexReader;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface ReaderAccessor {

	/**
	 * Opens an IndexReader on all indexes containing the entities passed as parameter.
	 * In the simplest case passing a single entity will map to a single index; if the entity
	 * uses a sharding strategy or if multiple entities using different index names are selected,
	 * the single IndexReader will act as a MultiReader on the aggregate of these indexes.
	 * This MultiReader is not filtered by Hibernate Search, so it might contain information
	 * relevant to different types as well.
	 * <p>The returned IndexReader is read only; writing directly to the index is discouraged, in need use the
	 * {@link org.hibernate.search.spi.SearchFactoryIntegrator#getWorker()} to queue change operations to the backend.</p>
	 * <p>The IndexReader should not be closed in other ways, but must be returned to this instance to
	 * {@link #closeIndexReader(IndexReader)}.</p>
	 * 
	 * @param entities
	 * @return an IndexReader containing at least all listed entities
	 */
	IndexReader openIndexReader(Class<?>... entities);

	/**
	 * Closes IndexReader instances obtained using {@link #openIndexReader(Class...)}
	 * @param indexReader the IndexReader to be closed
	 */
	void closeIndexReader(IndexReader indexReader);

}
