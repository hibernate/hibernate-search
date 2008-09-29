package org.hibernate.search.backend;

/**
 * A visitor delegate to manipulate a LuceneWork
 * needs to implement this interface.
 * This pattern enables any implementation to virtually add delegate
 * methods to the base LuceneWork without having to change them.
 * This contract however breaks if more subclasses of LuceneWork
 * are created, as a visitor must support all existing types.
 * 
 * @author Sanne Grinovero
 *
 * @param <T> used to force a return type of choice.
 */
public interface WorkVisitor<T> {

	T getDelegate(AddLuceneWork addLuceneWork);
	T getDelegate(DeleteLuceneWork deleteLuceneWork);
	T getDelegate(OptimizeLuceneWork optimizeLuceneWork);
	T getDelegate(PurgeAllLuceneWork purgeAllLuceneWork);

}
