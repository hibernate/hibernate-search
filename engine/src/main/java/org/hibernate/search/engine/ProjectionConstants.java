/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

/**
 * Defined projection constants.
 *
 * @author Emmanuel Bernard
 */
public interface ProjectionConstants {
	/**
	 * Represents the Hibernate entity returned in a search.
	 */
	String THIS = "__HSearch_This";

	/**
	 * The Lucene document returned by a search.
	 */
	String DOCUMENT = "__HSearch_Document";

	/**
	 * The legacy document's score from a search.
	 */
	String SCORE = "__HSearch_Score";

	/**
	 * Object id property
	 */
	String ID = "__HSearch_id";

	/**
	 * Lucene Document id.
	 * <p>
	 * Expert: Lucene document id can change overtime between 2 different IndexReader opening.
	 *
	 * @hsearch.experimental If you use this constant/feature, please speak up in the forum
	 */
	String DOCUMENT_ID = "__HSearch_DocumentId";

	/**
	 * Lucene {@link org.apache.lucene.search.Explanation} object describing the score computation for
	 * the matching object/document
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination)
	 * To retrieve explanation of a single result, consider retrieving {@link #DOCUMENT_ID}
	 * and using fullTextQuery.explain(int)
	 */
	String EXPLANATION = "__HSearch_Explanation";

	/**
	 * Represents the Hibernate entity class returned in a search. In contrast to the other constants this constant
	 * represents an actual field value of the underlying Lucene document and hence can directly be used in queries.
	 */
	String OBJECT_CLASS = "_hibernate_class";

	/**
	 * Represents the distance between an entity and the center of the search radius in case of a spatial query
	 */
	String SPATIAL_DISTANCE = "_HSearch_SpatialDistance";
}
