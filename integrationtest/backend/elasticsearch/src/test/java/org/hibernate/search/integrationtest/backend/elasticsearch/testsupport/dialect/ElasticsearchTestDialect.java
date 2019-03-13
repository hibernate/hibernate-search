/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public interface ElasticsearchTestDialect {

	boolean isEmptyMappingPossible();

	URLEncodedString getTypeKeywordForNonMappingApi();

	Optional<URLEncodedString> getTypeNameForMappingApi();

	static ElasticsearchTestDialect get() {
		String dialectClassName = System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.testdialect" );
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ElasticsearchTestDialect> dialectClass =
					(Class<? extends ElasticsearchTestDialect>) Class.forName( dialectClassName );
			return dialectClass.getConstructor().newInstance();
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(
					"Unexpected error while initializing the ElasticsearchTestDialect with name " + dialectClassName,
					e
			);
		}
	}
}
