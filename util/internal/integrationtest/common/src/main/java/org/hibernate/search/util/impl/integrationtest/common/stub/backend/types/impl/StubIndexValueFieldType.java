/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchValueFieldTypeContext;

public final class StubIndexValueFieldType<F> implements IndexFieldType<F>, StubSearchValueFieldTypeContext<F> {

	private final Class<F> valueClass;
	private final DslConverter<F, F> rawDslConverter;
	private final DslConverter<?, F> dslConverter;
	private final ProjectionConverter<F, F> rawProjectionConverter;
	private final ProjectionConverter<F, ?> projectionConverter;
	private final List<Consumer<StubIndexSchemaDataNode.Builder>> modifiers;

	public StubIndexValueFieldType(Class<F> valueClass,
			DslConverter<?, F> dslConverter,
			ProjectionConverter<F, ?> projectionConverter,
			List<Consumer<StubIndexSchemaDataNode.Builder>> modifiers) {
		this.valueClass = valueClass;
		this.rawDslConverter = new DslConverter<>( valueClass, new PassThroughToDocumentFieldValueConverter<>() );
		this.dslConverter = dslConverter != null ? dslConverter : rawDslConverter;
		this.rawProjectionConverter = new ProjectionConverter<>(
				valueClass, new PassThroughFromDocumentFieldValueConverter<>() );
		this.projectionConverter = projectionConverter != null ? projectionConverter : rawProjectionConverter;
		this.modifiers = modifiers;
	}

	public void apply(StubIndexSchemaDataNode.Builder builder) {
		builder.valueClass( valueClass );
		for ( Consumer<StubIndexSchemaDataNode.Builder> modifier : modifiers ) {
			modifier.accept( builder );
		}
	}

	@Override
	public Class<F> valueClass() {
		return valueClass;
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return dslConverter;
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return rawDslConverter;
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return projectionConverter;
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return rawProjectionConverter;
	}

	@Override
	public <T> AbstractStubSearchQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return StubSearchQueryElementFactories.get( key );
	}
}
