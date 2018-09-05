/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch;

/**
 * Projection constants specific to Elasticsearch.
 * <p>
 * Implementator's note: When adding new constants, be sure to add them to
 * {@code ElasticsearchHSQueryImpl#SUPPORTED_PROJECTION_CONSTANTS}, too.
 *
 * @author Gunnar Morling
 */
public interface ElasticsearchProjectionConstants {

	/**
	 * Represents the Hibernate entity returned in a search.
	 */
	String THIS = "__HSearch_This";

	/**
	 * Object id property
	 */
	String ID = "__HSearch_id";

	/**
	 * The legacy document's score from a search.
	 */
	String SCORE = "__HSearch_Score";

	/**
	 * Represents the Hibernate entity class returned in a search. In contrast to the other constants this constant
	 * represents an actual field value of the underlying Lucene document and hence can directly be used in queries.
	 */
	String OBJECT_CLASS = "_hibernate_class";

	/**
	 * Represents the distance (in kilometers) between an entity and the
	 * center of the search area in case of a spatial query.
	 */
	// TODO HSEARCH-2268: Make it start with "__"
	String SPATIAL_DISTANCE = "_HSearch_SpatialDistance";

	/**
	 * The JSON document as stored in Elasticsearch.
	 */
	String SOURCE = "__HSearch_Source";

	/**
	 * The time Elasticsearch took to execute the search, in milliseconds and as an {@code Integer}.
	 * <p>The time is computed on the server side.
	 */
	String TOOK = "__HSearch_Took";

	/**
	 * Whether the search timed out on the Elasticsearch server or not, as a {@code Boolean}.
	 * <p>Note that a timed-out search may still return results, they are just incomplete.
	 * <p>This does not translate a network timeout while reaching out to the Elasticsearch server,
	 * but a timeout internal to Elasticsearch itself.
	 */
	String TIMED_OUT = "__HSearch_TimedOut";
}
