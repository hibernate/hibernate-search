package org.hibernate.search.backend.impl.lucene;

/**
 * Constants to make the LuceneWorkDelegates advertise the type
 * of resource they are going to need to perform the work.
 * 
 * NEEDS_INDEXREADER is missing to make sure there always is an optimal
 * solution, as some operations can be done both through an IndexReader
 * and an IndexWriter, but as of Lucene 2.4 there are no operations which
 * can't be done using an IndexWriter.
 * 
 * @author Sanne Grinovero
 */
public enum IndexInteractionType {
	
	/**
	 * The workType needs an IndexWriter.
	 */
	NEEDS_INDEXWRITER,
	/**
	 * An IndexWriter should work best but it's possible
	 * to use an IndexReader instead.
	 */
	PREFER_INDEXWRITER,
	/**
	 * An IndexReader should work best but it's possible
	 * to use an IndexWriter instead.
	 */
	PREFER_INDEXREADER

}
