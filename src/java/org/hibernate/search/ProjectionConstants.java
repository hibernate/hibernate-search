// $Id$
package org.hibernate.search;

/**
 * Define Projection constants
 *
 * @author Emmanuel Bernard
 */
public interface ProjectionConstants {
	/**
	 * Represtnts the Hibernate Entity returned in a search.
	 */
	public String THIS = "__HSearch_This";
	/**
	 * The Lucene document returned by a search.
	 */
	public String DOCUMENT = "__HSearch_Document";
	/**
	 * The legacy document's score from a search.
	 */
	public String SCORE = "__HSearch_Score";
	/**
	 * The boost value of the Lucene document.
	 * @deprecated always return 1
	 */
	public String BOOST = "__HSearch_Boost";
	/**
	 * Object id property
	 */
	public String ID = "__HSearch_id";
	/**
	 * Lucene Document id
	 * Experimental: If you use this feature, please speak up in the forum
	 *  
	 * Expert: Lucene document id can change overtime between 2 different IndexReader opening.
	 */
	public String DOCUMENT_ID = "__HSearch_DocumentId";
	/**
	 * Object class
	 */
	//TODO OBJECT CLASS
}
