/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.Optional;

import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;

import com.google.gson.JsonPrimitive;

public interface ElasticsearchSearchIndexValueFieldTypeContext<F>
		extends
		SearchIndexValueFieldTypeContext<ElasticsearchSearchIndexScope, ElasticsearchSearchIndexValueFieldContext<F>, F> {

	JsonPrimitive elasticsearchTypeAsJson();

	Optional<String> searchAnalyzerName();

	Optional<String> normalizerName();

	boolean hasNormalizerOnAtLeastOneIndex();

}
