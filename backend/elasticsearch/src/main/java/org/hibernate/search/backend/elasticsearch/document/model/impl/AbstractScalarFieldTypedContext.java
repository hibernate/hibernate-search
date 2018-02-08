/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractScalarFieldTypedContext<T> extends AbstractElasticsearchIndexSchemaFieldTypedContext<T> {

	private Store store = Store.DEFAULT;

	public AbstractScalarFieldTypedContext() {
	}

	@Override
	public IndexSchemaFieldTypedContext<T> store(Store store) {
		this.store = store;
		return this;
	}

	@Override
	protected PropertyMapping contribute(
			DeferredInitializationIndexFieldAccessor<T> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = new PropertyMapping();

		// TODO set docvalues if sortable

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

		return mapping;
	}
}
