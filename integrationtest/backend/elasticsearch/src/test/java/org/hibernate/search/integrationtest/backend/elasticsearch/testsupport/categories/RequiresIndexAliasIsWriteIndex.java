/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories;

/**
 * JUnit category marker indicating that a test is only relevant for Elasticsearch versions supporting the is_write_index
 * attribute in alias definitions.
 * <p>
 * These operations are not available on ES 6.3 and below in particular.
 */
public class RequiresIndexAliasIsWriteIndex {
}
