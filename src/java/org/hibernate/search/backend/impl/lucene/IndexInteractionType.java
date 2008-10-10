package org.hibernate.search.backend.impl.lucene;

/**
 * @author Sanne Grinovero
 */
public enum IndexInteractionType {
	
	/**
	 * means the workType needs an IndexWriter.
	 */
	NEEDS_INDEXWRITER,
	/**
	 * means the workType needs an IndexReader.
	 */
	NEEDS_INDEXREADER,
	/**
	 * means an IndexWriter should work best but it's possible
	 * to use an IndexReader instead.
	 */
	PREFER_INDEXWRITER,
	/**
	 * means an IndexReader should work best but it's possible
	 * to use an IndexWriter instead.
	 */
	PREFER_INDEXREADER

}
