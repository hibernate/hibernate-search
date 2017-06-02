/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil.junit;


/**
 * JUnit category marker.
 * <p>
 * Used to ignore tests which are not sensible when testing against
 * an Elasticsearch version higher than or equal to 5.0.0.
 */
public class SkipFromElasticsearch50 {

}
