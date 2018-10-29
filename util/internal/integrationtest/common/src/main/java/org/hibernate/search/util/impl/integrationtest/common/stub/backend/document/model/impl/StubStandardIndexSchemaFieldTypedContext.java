/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

abstract class StubStandardIndexSchemaFieldTypedContext<S extends StubStandardIndexSchemaFieldTypedContext<? extends S, F>, F>
		implements StandardIndexSchemaFieldTypedContext<S, F> {

	private IndexSchemaFieldDefinitionHelper<F> helper;
	protected final StubIndexSchemaNode.Builder builder;
	private final boolean included;

	StubStandardIndexSchemaFieldTypedContext(StubIndexSchemaNode.Builder builder, Class<F> inputType, boolean included) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( builder, inputType );
		this.builder = builder;
		this.included = included;
		builder.inputType( inputType );
	}

	abstract S thisAsS();

	@Override
	public S dslConverter(
			ToIndexFieldValueConverter<?, ? extends F> toIndexConverter) {
		return thisAsS();
	}

	@Override
	public S projectionConverter(
			FromIndexFieldValueConverter<? super F, ?> fromIndexConverter) {
		return thisAsS();
	}

	@Override
	public S projectable(Projectable projectable) {
		builder.projectable( projectable );
		return thisAsS();
	}

	@Override
	public S sortable(Sortable sortable) {
		builder.sortable( sortable );
		return thisAsS();
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		IndexFieldAccessor<F> accessor = helper.createAccessor();
		if ( included ) {
			helper.initialize( new StubIncludedIndexFieldAccessor<>( builder.getAbsolutePath(), builder.getRelativeName() ) );
		}
		else {
			helper.initialize( new StubExcludedIndexFieldAccessor<>( builder.getAbsolutePath(), builder.getRelativeName() ) );
		}
		return accessor;
	}

}
