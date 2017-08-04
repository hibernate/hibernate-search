/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.NonConvertingElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

/**
 * @author Yoann Rodiere
 */
class IntegerFieldModelContext extends AbstractScalarFieldModelContext<Integer> {

	private final JsonAccessor<Integer> accessor;

	public IntegerFieldModelContext(JsonAccessor<Integer> accessor) {
		this.accessor = accessor;
	}

	@Override
	protected void build(DeferredInitializationIndexFieldReference<Integer> reference, PropertyMapping mapping) {
		super.build( reference, mapping );
		reference.initialize( new NonConvertingElasticsearchIndexFieldReference<>( accessor ) );
		mapping.setType( DataType.INTEGER );
	}
}
