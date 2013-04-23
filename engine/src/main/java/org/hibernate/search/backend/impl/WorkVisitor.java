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
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;

/**
 * A visitor delegate to manipulate a LuceneWork
 * needs to implement this interface.
 * This pattern enables any implementation to virtually add delegate
 * methods to the base LuceneWork without having to change them.
 * This contract however breaks if more subclasses of LuceneWork
 * are created, as a visitor must support all existing types.
 *
 * @author Sanne Grinovero
 * @param <T> used to force a return type of choice.
 */
public interface WorkVisitor<T> {

	T getDelegate(AddLuceneWork addLuceneWork);
	T getDelegate(DeleteLuceneWork deleteLuceneWork);
	T getDelegate(OptimizeLuceneWork optimizeLuceneWork);
	T getDelegate(PurgeAllLuceneWork purgeAllLuceneWork);
	T getDelegate(UpdateLuceneWork updateLuceneWork);
	T getDelegate(FlushLuceneWork flushLuceneWork);

}
