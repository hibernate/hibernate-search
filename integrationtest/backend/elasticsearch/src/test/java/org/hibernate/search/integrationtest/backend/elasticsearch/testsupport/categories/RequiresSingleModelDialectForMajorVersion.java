/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories;

/**
 * JUnit category marker indicating that a test is only relevant
 * for Elasticsearch major versions where all minor versions
 * use the same model dialect.
 * <p>
 * It is not the case on ES 5 in particular, since 5.6 has a dialect
 * but 5.0, 5.1, etc. don't have one.
 */
public class RequiresSingleModelDialectForMajorVersion {
}
