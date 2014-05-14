/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
