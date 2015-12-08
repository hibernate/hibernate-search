/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

/**
 * Projection constants specific to the ElasticSearch backend.
 *
 * @author Gunnar Morling
 */
public interface ProjectionConstants extends org.hibernate.search.engine.ProjectionConstants {

	/**
	 * The JSON document as stored in ElasticSearch.
	 */
	String SOURCE = "__HSearch_Source";

	// TODO "Took" etc.?
}
