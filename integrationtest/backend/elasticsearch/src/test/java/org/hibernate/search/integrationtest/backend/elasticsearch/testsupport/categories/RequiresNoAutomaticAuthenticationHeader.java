/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories;

/**
 * JUnit category marker indicating that a test is only relevant
 * if Elasticsearch request are <strong>not</strong> automatically
 * augmented with an "Authentication:" header.
 * <p>
 * "Authentication:" headers are added by the AWS integration in particular.
 */
public class RequiresNoAutomaticAuthenticationHeader {
}
