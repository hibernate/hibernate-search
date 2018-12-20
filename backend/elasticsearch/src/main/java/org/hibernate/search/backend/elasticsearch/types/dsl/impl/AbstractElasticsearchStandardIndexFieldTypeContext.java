/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchStandardIndexFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.AssertionFailure;

/**
 * @author Yoann Rodiere
 */
public abstract class AbstractElasticsearchStandardIndexFieldTypeContext<S extends AbstractElasticsearchStandardIndexFieldTypeContext<? extends S, F>, F>
		implements ElasticsearchStandardIndexFieldTypeContext<S, F>,
		ElasticsearchIndexSchemaNodeContributor<PropertyMapping> {

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	AbstractElasticsearchStandardIndexFieldTypeContext(IndexSchemaBuildContext schemaContext, Class<F> fieldType) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, fieldType );
	}

	@Override
	public S dslConverter(
			ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		helper.dslConverter( toIndexConverter );
		return thisAsS();
	}

	@Override
	public S projectionConverter(
			FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		helper.projectionConverter( fromIndexConverter );
		return thisAsS();
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		return helper.createAccessor();
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		return contribute( helper, collector, parentNode );
	}

	protected abstract PropertyMapping contribute(IndexSchemaFieldDefinitionHelper<F> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode);

	protected abstract S thisAsS();

	protected final IndexSchemaBuildContext getSchemaContext() {
		return helper.getSchemaContext();
	}

	protected static boolean resolveDefault(Projectable projectable) {
		switch ( projectable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Projectable: " + projectable );
		}
	}

	protected static boolean resolveDefault(Sortable sortable) {
		switch ( sortable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Sortable: " + sortable );
		}
	}
}
