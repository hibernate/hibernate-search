/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.analysis;

public final class Analyzers {

	private Analyzers() {
	}

	/**
	 * tokenizer: standard
	 * token filters: lowercase, snowball-porter(english), asciifolding
	 */
	public static final String ANALYZER_ENGLISH = "english";

	/**
	 * token filters: lowercase, asciifolding
	 */
	public static final String NORMALIZER_ENGLISH = "english";

}
