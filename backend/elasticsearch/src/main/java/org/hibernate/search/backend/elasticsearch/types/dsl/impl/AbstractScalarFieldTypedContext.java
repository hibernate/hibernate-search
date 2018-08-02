/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractScalarFieldTypedContext<F> extends AbstractElasticsearchIndexSchemaFieldTypedContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String relativeFieldName;
	private final DataType dataType;
	private Store store = Store.DEFAULT;
	private Sortable sortable = Sortable.DEFAULT;

	AbstractScalarFieldTypedContext(IndexSchemaContext schemaContext,
			String relativeFieldName, Class<F> fieldType, DataType dataType) {
		super( schemaContext, fieldType );
		this.relativeFieldName = relativeFieldName;
		this.dataType = dataType;
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> analyzer(String analyzerName) {
		throw log.cannotUseAnalyzerOnFieldType(
				relativeFieldName, dataType, getSchemaContext().getEventContext()
		);
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> normalizer(String normalizerName) {
		throw log.cannotUseNormalizerOnFieldType(
				relativeFieldName, dataType, getSchemaContext().getEventContext()
		);
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<F> sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected PropertyMapping contribute(
			IndexSchemaFieldDefinitionHelper<F> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = new PropertyMapping();

		mapping.setType( dataType );

		switch ( store ) {
			case DEFAULT:
				break;
			case NO:
				mapping.setStore( false );
				break;
			case YES:
			case COMPRESS:
				// TODO what about Store.COMPRESS?
				mapping.setStore( true );
				break;
		}

		switch ( sortable ) {
			case DEFAULT:
				break;
			case NO:
				mapping.setDocValues( false );
				break;
			case YES:
				mapping.setDocValues( true );
				break;
		}

		return mapping;
	}

}
