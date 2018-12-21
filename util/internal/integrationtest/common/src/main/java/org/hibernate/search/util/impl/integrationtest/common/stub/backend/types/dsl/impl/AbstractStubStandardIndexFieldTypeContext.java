/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubExcludedIndexFieldAccessor;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIncludedIndexFieldAccessor;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;

abstract class AbstractStubStandardIndexFieldTypeContext<S extends AbstractStubStandardIndexFieldTypeContext<? extends S, F>, F>
		implements StandardIndexFieldTypeContext<S, F> {

	private IndexSchemaFieldDefinitionHelper<F> helper;
	protected final StubIndexSchemaNode.Builder builder;
	private final Class<F> inputType;
	private final boolean included;

	private ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromDocumentFieldValueConverter<? super F, ?> projectionFromIndexConverter;

	AbstractStubStandardIndexFieldTypeContext(StubIndexSchemaNode.Builder builder, Class<F> inputType, boolean included) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( builder );
		this.builder = builder;
		this.inputType = inputType;
		this.included = included;
		builder.inputType( inputType );
	}

	abstract S thisAsS();

	@Override
	public S dslConverter(ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter) {
		this.dslToIndexConverter = toIndexConverter;
		return thisAsS();
	}

	@Override
	public S projectionConverter(FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter) {
		this.projectionFromIndexConverter = fromIndexConverter;
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
		builder.converter( new StubFieldConverter<>(
				inputType,
				dslToIndexConverter == null
						? new PassThroughToDocumentFieldValueConverter<>( inputType )
						: dslToIndexConverter,
				projectionFromIndexConverter == null
						? new PassThroughFromDocumentFieldValueConverter<>( inputType )
						: projectionFromIndexConverter
		) );
		return accessor;
	}

}
