/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import org.hibernate.search.spatial.Coordinates;

import com.google.gson.JsonObject;

/**
 * An object responsible for creating parts of an Elasticsearch JSON query.
 * <p>
 * Should eventually be responsible for building the whole JSON query,
 * see HSEARCH-2750.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HSEARCH-2750">HSEARCH-2750</a>
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchQueryFactory {

	JsonObject createSpatialDistanceScript(Coordinates center, String spatialFieldName);

}
