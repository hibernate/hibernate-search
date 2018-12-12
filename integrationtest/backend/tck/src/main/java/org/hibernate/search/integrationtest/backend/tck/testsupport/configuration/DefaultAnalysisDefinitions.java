/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.configuration;

/**
 * The analysis definitions that are expected to be present in the default configuration.
 * <p>
 * Access {@link #name} to get the expected name .
 * See the javadoc for a description of what is expected in each definition.
 */
public enum DefaultAnalysisDefinitions {
	/**
	 * The standard analyzer, as defined by Lucene.
	 */
	ANALYZER_STANDARD("standard"),
	/**
	 * A normalizer that lowercases the input.
	 */
	NORMALIZER_LOWERCASE("lowercase")
	;

	public final String name;

	DefaultAnalysisDefinitions(String suffix) {
		this.name = getClass().getSimpleName() + "_" + suffix;
	}
}
