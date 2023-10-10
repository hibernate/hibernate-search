/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

public interface Tags {
	/**
	 * JUnit category marker.
	 * <p>
	 * Used to mark tests that will never be ported over to Search 6.
	 * <p>
	 * Please be nice and add a comment explaining why you decide not to port a given test...
	 */
	String WILL_NOT_PORT_TO_SEARCH_6 = "WillNotPortToSearch6";
	/**
	 * JUnit category marker.
	 *
	 * Used to temporarily ignore tests which are not working yet with the Elasticsearch backend.
	 */
	String ELASTICSEARCH_SUPPORT_IN_PROGRESS = "ElasticsearchSupportInProgress";
	/**
	 * JUnit category marker.
	 *
	 * Used to ignore tests which are not sensible when using the Elasticsearch backend.
	 */
	String SKIP_ON_ELASTICSEARCH = "SkipOnElasticsearch";
	/**
	 * JUnit category marker.
	 * <p>
	 * Used to mark tests that have already been ported over to Search 6.
	 */
	String PORTED_TO_SEARCH_6 = "PortedToSearch6";
}
