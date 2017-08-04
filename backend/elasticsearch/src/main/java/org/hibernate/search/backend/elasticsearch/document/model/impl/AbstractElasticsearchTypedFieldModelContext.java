/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchTypedFieldModelContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;

/**
 * @author Yoann Rodiere
 */
public abstract class AbstractElasticsearchTypedFieldModelContext<T>
		implements ElasticsearchTypedFieldModelContext<T>, ElasticsearchMappingContributor<PropertyMapping> {

	private DeferredInitializationIndexFieldReference<T> reference = new DeferredInitializationIndexFieldReference<>();

	@Override
	public IndexFieldReference<T> asReference() {
		return reference;
	}

	@Override
	public final void contribute(PropertyMapping mapping) {
		build( reference, mapping );
	}

	protected abstract void build(DeferredInitializationIndexFieldReference<T> reference, PropertyMapping mapping);

}
