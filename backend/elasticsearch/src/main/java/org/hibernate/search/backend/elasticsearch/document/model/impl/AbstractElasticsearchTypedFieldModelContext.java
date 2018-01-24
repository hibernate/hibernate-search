/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchTypedFieldModelContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;

/**
 * @author Yoann Rodiere
 */
public abstract class AbstractElasticsearchTypedFieldModelContext<T>
		implements ElasticsearchTypedFieldModelContext<T>, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> {

	private DeferredInitializationIndexFieldAccessor<T> reference = new DeferredInitializationIndexFieldAccessor<>();

	@Override
	public IndexFieldAccessor<T> createAccessor() {
		return reference;
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchObjectNodeModel parentModel) {
		return contribute( reference, collector, parentModel );
	}

	protected abstract PropertyMapping contribute(DeferredInitializationIndexFieldAccessor<T> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchObjectNodeModel parentModel);

}
