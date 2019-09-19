/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexFieldType;

abstract class AbstractStubStandardIndexFieldTypeOptionsStep<S extends AbstractStubStandardIndexFieldTypeOptionsStep<?, F>, F>
		implements StandardIndexFieldTypeOptionsStep<S, F> {

	final List<Consumer<StubIndexSchemaNode.Builder>> modifiers;
	private final Class<F> inputType;

	private ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private FromDocumentFieldValueConverter<? super F, ?> projectionFromIndexConverter;

	AbstractStubStandardIndexFieldTypeOptionsStep(Class<F> inputType) {
		this.modifiers = new ArrayList<>();
		this.inputType = inputType;
		modifiers.add(
				b -> b.converter( new StubFieldConverter<>(
						inputType,
						dslToIndexConverter == null
								? new PassThroughToDocumentFieldValueConverter<>( inputType )
								: dslToIndexConverter,
						projectionFromIndexConverter == null
								? new PassThroughFromDocumentFieldValueConverter<>( inputType )
								: projectionFromIndexConverter
				) )
		);
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
		modifiers.add( b -> b.projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public S sortable(Sortable sortable) {
		modifiers.add( b -> b.sortable( sortable ) );
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAs) {
		modifiers.add( b -> b.indexNullAs( indexNullAs ) );
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		modifiers.add( b -> b.searchable( searchable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		modifiers.add( b -> b.aggregable( aggregable ) );
		return thisAsS();
	}

	@Override
	public IndexFieldType<F> toIndexFieldType() {
		return new StubIndexFieldType<>( inputType, modifiers );
	}

}
