/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.FieldDataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class IndexSchemaFieldStringContext extends AbstractElasticsearchIndexSchemaFieldTypedContext<String> {

	private final String relativeName;
	private Store store = Store.DEFAULT;

	public IndexSchemaFieldStringContext(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldAccessor<String> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = new PropertyMapping();

		ElasticsearchIndexSchemaFieldNode node = new ElasticsearchIndexSchemaFieldNode( parentNode, StringFieldFormatter.INSTANCE );

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeName );
		reference.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );
		// TODO auto-select type, or use sub-fields (but in that case, adjust projections accordingly)
		if ( false ) {
			mapping.setType( DataType.TEXT );
			switch ( store ) {
				case DEFAULT:
					break;
				case NO:
					mapping.setFieldData( FieldDataType.FALSE );
					break;
				case YES:
				case COMPRESS:
					// TODO what about Store.COMPRESS?
					mapping.setFieldData( FieldDataType.TRUE );
					break;
			}
		}
		else {
			mapping.setType( DataType.KEYWORD );
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
		}

		String absolutePath = parentNode.getAbsolutePath( relativeName );
		collector.collect( absolutePath, node );

		return mapping;
	}

	private static final class StringFieldFormatter implements ElasticsearchFieldFormatter {
		// Must be a singleton so that equals() works as required by the interface
		public static final StringFieldFormatter INSTANCE = new StringFieldFormatter();

		private StringFieldFormatter() {
		}

		@Override
		public JsonElement format(Object object) {
			if ( object == null ) {
				return JsonNull.INSTANCE;
			}
			String value = (String) object;
			return new JsonPrimitive( value );
		}

		@Override
		public Object parse(JsonElement element) {
			if ( element == null || element.isJsonNull() ) {
				return null;
			}
			return JsonElementType.STRING.fromElement( element );
		}
	}
}
