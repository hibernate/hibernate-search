/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.document.model.spi.TypedFieldModelContext;
import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.NonConvertingElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.FieldDataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

/**
 * @author Yoann Rodiere
 */
class StringFieldModelContext extends AbstractElasticsearchTypedFieldModelContext<String> {

	private final JsonAccessor<String> accessor;
	private Store store;

	public StringFieldModelContext(JsonAccessor<String> accessor) {
		this.accessor = accessor;
	}

	@Override
	public TypedFieldModelContext<String> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	protected void build(DeferredInitializationIndexFieldReference<String> reference, PropertyMapping mapping) {
		reference.initialize( new NonConvertingElasticsearchIndexFieldReference<>( accessor ) );
		// TODO auto-select type, or use sub-fields (but in that case, adjust projections accordingly)
		if ( false ) {
			mapping.setType( DataType.TEXT );
			if ( store != null && Store.NO.equals( store ) ) {
				// TODO what about Store.COMPRESS?
				mapping.setFieldData( FieldDataType.TRUE );
			}
		}
		else {
			mapping.setType( DataType.KEYWORD );
			if ( store != null && Store.NO.equals( store ) ) {
				// TODO what about Store.COMPRESS?
				mapping.setStore( true );
			}
		}
	}
}
