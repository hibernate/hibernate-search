/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.FieldDataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.StringFieldCodec;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class StringIndexSchemaFieldContext extends AbstractElasticsearchIndexSchemaFieldTypedContext<String> {

	private final String relativeName;
	private String analyzerName;
	private String normalizerName;
	private Store store = Store.DEFAULT;
	private Sortable sortable = Sortable.DEFAULT;

	public StringIndexSchemaFieldContext(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldAccessor<String> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = new PropertyMapping();

		ElasticsearchIndexSchemaFieldNode node = new ElasticsearchIndexSchemaFieldNode( parentNode, StringFieldCodec.INSTANCE );

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeName );
		reference.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );
		// TODO Use sub-fields? (but in that case, adjust projections accordingly)
		if ( analyzerName != null ) {
			mapping.setType( DataType.TEXT );
			mapping.setAnalyzer( analyzerName );

			switch ( sortable ) {
				case DEFAULT:
					break;
				case NO:
					mapping.setFieldData( FieldDataType.FALSE );
					break;
				case YES:
					mapping.setFieldData( FieldDataType.TRUE );
					break;
			}
		}
		else {
			mapping.setType( DataType.KEYWORD );
			mapping.setNormalizer( normalizerName );

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
		}

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

		String absolutePath = parentNode.getAbsolutePath( relativeName );
		collector.collect( absolutePath, node );

		return mapping;
	}
}
