/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public class Elasticsearch7TestDialect implements ElasticsearchTestDialect {
	private final URLEncodedString _DOC = URLEncodedString.fromString( "_doc" );

	@Override
	public boolean isEmptyMappingPossible() {
		return false;
	}

	@Override
	public URLEncodedString getTypeKeywordForNonMappingApi() {
		return _DOC;
	}

	@Override
	public Optional<URLEncodedString> getTypeNameForMappingApi() {
		return Optional.empty();
	}
}
