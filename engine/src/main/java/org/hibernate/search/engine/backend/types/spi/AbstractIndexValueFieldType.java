/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;

public abstract class AbstractIndexValueFieldType<
		SC extends SearchIndexScope<?>,
		N extends SearchIndexValueFieldContext<SC>,
		F>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, SearchIndexValueFieldTypeContext<SC, N, F> {
	private final Class<F> valueClass;
	private final DslConverter<F, F> rawDslConverter;
	private final ProjectionConverter<F, F> rawProjectionConverter;
	private final DslConverter<?, F> dslConverter;
	private final ProjectionConverter<F, ?> projectionConverter;

	private final boolean searchable;
	private final boolean sortable;
	private final boolean projectable;
	private final boolean aggregable;
	private final Set<SearchHighlighterType> allowedHighlighterTypes;

	private final Map<SearchQueryElementTypeKey<?>, SearchQueryElementFactory<?, ? super SC, ? super N>> queryElementFactories;

	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	protected AbstractIndexValueFieldType(Builder<SC, N, F> builder) {
		this.valueClass = builder.valueClass;
		this.rawDslConverter = builder.rawDslConverter;
		this.rawProjectionConverter = builder.rawProjectionConverter;
		this.dslConverter = builder.dslConverter != null ? builder.dslConverter : rawDslConverter;
		this.projectionConverter = builder.projectionConverter != null ? builder.projectionConverter : rawProjectionConverter;
		this.searchable = builder.searchable;
		this.sortable = builder.sortable;
		this.projectable = builder.projectable;
		this.aggregable = builder.aggregable;
		this.allowedHighlighterTypes = Collections.unmodifiableSet( builder.allowedHighlighterTypes );
		this.queryElementFactories = builder.queryElementFactories;
		this.analyzerName = builder.analyzerName;
		this.searchAnalyzerName = builder.searchAnalyzerName != null ? builder.searchAnalyzerName : builder.analyzerName;
		this.normalizerName = builder.normalizerName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "valueClass=" + valueClass.getName()
				+ ", analyzerName=" + analyzerName
				+ ", searchAnalyzerName=" + searchAnalyzerName
				+ ", normalizerName=" + normalizerName
				+ ", capabilities=" + queryElementFactories.keySet()
				+ "]";
	}

	@Override
	public final Class<F> valueClass() {
		return valueClass;
	}

	@Override
	public final boolean searchable() {
		return searchable;
	}

	@Override
	public final boolean sortable() {
		return sortable;
	}

	@Override
	public final boolean projectable() {
		return projectable;
	}

	@Override
	public final boolean aggregable() {
		return aggregable;
	}

	@Override
	public final Class<?> dslArgumentClass() {
		return dslConverter.valueType();
	}

	@Override
	public final DslConverter<?, F> dslConverter() {
		return dslConverter;
	}

	@Override
	public final DslConverter<F, F> rawDslConverter() {
		return rawDslConverter;
	}

	@Override
	public final Class<?> projectedValueClass() {
		return projectionConverter.valueType();
	}

	@Override
	public final ProjectionConverter<F, ?> projectionConverter() {
		return projectionConverter;
	}

	@Override
	public final ProjectionConverter<F, F> rawProjectionConverter() {
		return rawProjectionConverter;
	}

	@Override
	public final Optional<String> analyzerName() {
		return Optional.ofNullable( analyzerName );
	}

	@Override
	public final Optional<String> normalizerName() {
		return Optional.ofNullable( normalizerName );
	}

	@Override
	public final Optional<String> searchAnalyzerName() {
		return Optional.ofNullable( searchAnalyzerName );
	}

	@SuppressWarnings("unchecked") // The cast is safe by construction; see the builder.
	@Override
	public final <T> SearchQueryElementFactory<? extends T, ? super SC, ? super N> queryElementFactory(
			SearchQueryElementTypeKey<T> key) {
		return (SearchQueryElementFactory<? extends T, ? super SC, ? super N>) queryElementFactories.get( key );
	}

	@Override
	public boolean highlighterTypeSupported(SearchHighlighterType type) {
		return allowedHighlighterTypes.contains( type );
	}

	public abstract static class Builder<
			SC extends SearchIndexScope<?>,
			N extends SearchIndexValueFieldContext<SC>,
			F> {

		private final Class<F> valueClass;
		private final DslConverter<F, F> rawDslConverter;
		private final ProjectionConverter<F, F> rawProjectionConverter;

		private DslConverter<?, F> dslConverter;
		private ProjectionConverter<F, ?> projectionConverter;

		private boolean searchable;
		private boolean sortable;
		private boolean projectable;
		private boolean aggregable;
		private Set<SearchHighlighterType> allowedHighlighterTypes = Collections.emptySet();

		private final Map<SearchQueryElementTypeKey<?>,
				SearchQueryElementFactory<?, ? super SC, ? super N>> queryElementFactories = new HashMap<>();

		private String analyzerName;
		private String searchAnalyzerName;
		private String normalizerName;

		public Builder(Class<F> valueClass) {
			this.valueClass = valueClass;
			this.rawDslConverter = DslConverter.passThrough( valueClass );
			this.rawProjectionConverter = ProjectionConverter.passThrough( valueClass );
		}

		public final Class<F> valueClass() {
			return valueClass;
		}

		public final <V> void dslConverter(Class<V> valueType, ToDocumentValueConverter<V, ? extends F> toIndexConverter) {
			this.dslConverter = new DslConverter<>( valueType, toIndexConverter );
		}

		public final <V> void projectionConverter(Class<V> valueType,
				FromDocumentValueConverter<? super F, V> fromIndexConverter) {
			this.projectionConverter = new ProjectionConverter<>( valueType, fromIndexConverter );
		}

		public final void searchable(boolean searchable) {
			this.searchable = searchable;
		}

		public final void sortable(boolean sortable) {
			this.sortable = sortable;
		}

		public final void projectable(boolean projectable) {
			this.projectable = projectable;
		}

		public final void aggregable(boolean aggregable) {
			this.aggregable = aggregable;
		}

		public final void allowedHighlighterTypes(Set<SearchHighlighterType> allowedHighlighterTypes) {
			this.allowedHighlighterTypes = allowedHighlighterTypes;
		}

		public final <T> void queryElementFactory(SearchQueryElementTypeKey<T> key,
				SearchQueryElementFactory<? extends T, ? super SC, ? super N> factory) {
			queryElementFactories.put( key, factory );
		}

		public final void analyzerName(String analyzerName) {
			this.analyzerName = analyzerName;
		}

		public final void searchAnalyzerName(String searchAnalyzerName) {
			this.searchAnalyzerName = searchAnalyzerName;
		}

		public final void normalizerName(String normalizerName) {
			this.normalizerName = normalizerName;
		}

		public abstract AbstractIndexValueFieldType<SC, N, F> build();
	}
}
