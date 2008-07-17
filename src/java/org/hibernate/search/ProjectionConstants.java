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
	 * Lucene {@link org.apache.lucene.search.Explanation} object describing the score computation for
	 * the matching object/document
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination)
	 * To retrieve explanation of a single result, consider retrieving {@link #DOCUMENT_ID}
	 * and using fullTextQuery.explain(int)
	 */
	public String EXPLANATION = "__HSearch_Explanation";
	
	/**
	 * Object class
	 */
	//TODO OBJECT CLASS
}
