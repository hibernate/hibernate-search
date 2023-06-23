/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

import com.google.gson.JsonPrimitive;

public class ElasticsearchMultiIndexSearchIndexValueFieldContext<F>
		extends AbstractMultiIndexSearchIndexValueFieldContext<
				ElasticsearchSearchIndexValueFieldContext<F>,
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchSearchIndexValueFieldTypeContext<F>,
				F>
		implements ElasticsearchSearchIndexValueFieldContext<F>, ElasticsearchSearchIndexValueFieldTypeContext<F> {

	public ElasticsearchMultiIndexSearchIndexValueFieldContext(ElasticsearchSearchIndexScope<?> scope, String absolutePath,
			List<? extends ElasticsearchSearchIndexValueFieldContext<F>> fieldForEachIndex) {
		super( scope, absolutePath, fieldForEachIndex );
	}

	@Override
	protected ElasticsearchSearchIndexValueFieldContext<F> self() {
		return this;
	}

	@Override
	protected ElasticsearchSearchIndexValueFieldTypeContext<F> selfAsNodeType() {
		return this;
	}

	@Override
	protected ElasticsearchSearchIndexValueFieldTypeContext<F> typeOf(
			ElasticsearchSearchIndexValueFieldContext<F> indexElement) {
		return indexElement.type();
	}

	@Override
	public ElasticsearchSearchIndexCompositeNodeContext toComposite() {
		return SearchIndexSchemaElementContextHelper.throwingToComposite( this );
	}

	@Override
	public ElasticsearchSearchIndexCompositeNodeContext toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public JsonPrimitive elasticsearchTypeAsJson() {
		return fromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::elasticsearchTypeAsJson, Object::equals,
				"elasticsearchType" );
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return fromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::searchAnalyzerName, Object::equals,
				"searchAnalyzer" );
	}

	@Override
	public Optional<String> normalizerName() {
		return fromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::normalizerName, Object::equals,
				"normalizer" );
	}

	@Override
	public boolean hasNormalizerOnAtLeastOneIndex() {
		for ( ElasticsearchSearchIndexValueFieldContext<F> indexElement : nodeForEachIndex ) {
			if ( indexElement.type().hasNormalizerOnAtLeastOneIndex() ) {
				return true;
			}
		}
		return false;
	}
}
