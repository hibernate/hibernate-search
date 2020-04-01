/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.util.List;
import java.util.Map;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFilterOptionsStep;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIndexFilterReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFilterBuilder<F extends FilterFactory>
	implements IndexSchemaFilterOptionsStep<StubIndexSchemaFilterBuilder<F>, IndexFilterReference<F>> {

	private IndexFilterReference<F> reference;
	private final StubIndexSchemaNode.Builder builder;

	StubIndexSchemaFilterBuilder(StubIndexSchemaNode.Builder builder, boolean included) {
		this.builder = builder;
	}

	@Override
	public <T> StubIndexSchemaFilterBuilder param(String name, T value) {
		builder.filterParam( name, value );
		return this;
	}

	@Override
	public StubIndexSchemaFilterBuilder<F> params(Map<String, Object> params) {
		params.putAll( params );
		return this;
	}

	@Override
	public IndexFilterReference<F> toReference() {
		if ( reference == null ) {
			List<Object> attribute = builder.attribute( "" );
			F factory = (F) attribute.get( 0 );

			reference = new StubIndexFilterReference<>(
				builder.getAbsolutePath(), builder.getRelativeName(),
				factory );
		}
		return reference;
	}
}
