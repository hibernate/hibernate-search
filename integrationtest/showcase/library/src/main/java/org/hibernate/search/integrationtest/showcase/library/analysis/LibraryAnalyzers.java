/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.analysis;

public final class LibraryAnalyzers {

	public static final String ANALYZER_DEFAULT = "default"; // No definition, just use the default from Elasticsearch
	public static final String NORMALIZER_SORT = "asciifolding_lowercase";
	public static final String NORMALIZER_ISBN = "isbn";

	private LibraryAnalyzers() {
	}
}
