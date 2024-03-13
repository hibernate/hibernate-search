/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

abstract class AbstractStubIndexFieldTypeOptionsStep<S extends AbstractStubIndexFieldTypeOptionsStep<?, F>, F>
		implements IndexFieldTypeOptionsStep<S, F> {

	protected final StubIndexValueFieldType.Builder<F> builder;

	AbstractStubIndexFieldTypeOptionsStep(Class<F> inputType) {
		this.builder = new StubIndexValueFieldType.Builder<>( inputType );
	}

	abstract S thisAsS();

	@Override
	public <V> S dslConverter(Class<V> valueType, ToDocumentValueConverter<V, ? extends F> toIndexConverter) {
		builder.dslConverter( valueType, toIndexConverter );
		return thisAsS();
	}

	@Override
	public <V> S projectionConverter(Class<V> valueType, FromDocumentValueConverter<? super F, V> fromIndexConverter) {
		builder.projectionConverter( valueType, fromIndexConverter );
		return thisAsS();
	}

	@Override
	public S parser(ToDocumentValueConverter<String, ? extends F> parser) {
		builder.parser( parser );
		return thisAsS();
	}

	@Override
	public IndexFieldType<F> toIndexFieldType() {
		return builder.build();
	}

}
