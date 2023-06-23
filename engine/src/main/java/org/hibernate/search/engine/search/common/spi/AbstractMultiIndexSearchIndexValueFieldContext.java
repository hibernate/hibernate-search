/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.List;
import java.util.Objects;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;

public abstract class AbstractMultiIndexSearchIndexValueFieldContext<
		S extends SearchIndexValueFieldContext<SC>,
		SC extends SearchIndexScope<?>,
		FT extends SearchIndexValueFieldTypeContext<SC, S, F>,
		F>
		extends AbstractMultiIndexSearchIndexNodeContext<S, SC, FT>
		implements SearchIndexValueFieldContext<SC>, SearchIndexValueFieldTypeContext<SC, S, F> {
	public AbstractMultiIndexSearchIndexValueFieldContext(SC scope, String absolutePath,
			List<? extends S> fieldForEachIndex) {
		super( scope, absolutePath, fieldForEachIndex );
	}

	@Override
	public final FT type() {
		return selfAsNodeType();
	}

	@Override
	public final boolean isComposite() {
		return false;
	}

	@Override
	public boolean isObjectField() {
		return false;
	}

	@Override
	public final boolean isValueField() {
		return true;
	}

	@Override
	public SearchIndexCompositeNodeContext<SC> toComposite() {
		return SearchIndexSchemaElementContextHelper.throwingToComposite( this );
	}

	@Override
	public SearchIndexCompositeNodeContext<SC> toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public final S toValueField() {
		return self();
	}

	@Override
	final SearchIndexSchemaElementContextHelper helper() {
		return SearchIndexSchemaElementContextHelper.VALUE_FIELD;
	}

	@Override
	public final Class<F> valueClass() {
		return fromTypeIfCompatible( SearchIndexValueFieldTypeContext::valueClass, Objects::equals,
				"valueClass" );
	}

	@Override
	public final DslConverter<?, F> dslConverter() {
		return fromTypeIfCompatible( SearchIndexValueFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public final DslConverter<F, F> rawDslConverter() {
		return fromTypeIfCompatible( SearchIndexValueFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public final ProjectionConverter<F, ?> projectionConverter() {
		return fromTypeIfCompatible( SearchIndexValueFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public final ProjectionConverter<F, F> rawProjectionConverter() {
		return fromTypeIfCompatible( SearchIndexValueFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public boolean highlighterTypeSupported(SearchHighlighterType type) {
		return fromTypeIfCompatible(
				t -> t.highlighterTypeSupported( type ),
				Object::equals,
				"highlighterTypeSupported"
		);
	}
}
