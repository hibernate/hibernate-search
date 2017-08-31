/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 */
class IntegerFieldModelContext extends AbstractScalarFieldModelContext<Integer> {

	private final UnknownTypeJsonAccessor accessor;

	public IntegerFieldModelContext(UnknownTypeJsonAccessor accessor) {
		this.accessor = accessor;
	}

	@Override
	protected void build(DeferredInitializationIndexFieldReference<Integer> reference, PropertyMapping mapping) {
		super.build( reference, mapping );
		reference.initialize( new ElasticsearchIndexFieldReference<>( accessor, JsonPrimitive::new ) );
		mapping.setType( DataType.INTEGER );
	}
}
