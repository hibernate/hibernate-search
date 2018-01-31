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

	private Store store;

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

		if ( store != null && !store.equals( Store.NO ) ) {
			// TODO what about Store.COMPRESS?
			mapping.setStore( true );
		}

		return mapping;
	}
}
