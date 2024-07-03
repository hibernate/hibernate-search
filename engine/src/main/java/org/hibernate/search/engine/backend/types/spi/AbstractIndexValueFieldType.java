/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.spi;

import java.util.Collections;
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
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;

public abstract class AbstractIndexValueFieldType<
		SC extends SearchIndexScope<?>,
		N extends SearchIndexValueFieldContext<SC>,
		F>
		extends AbstractIndexNodeType<SC, N>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, SearchIndexValueFieldTypeContext<SC, N, F> {
	private final Class<F> valueClass;
	private final DslConverter<F, F> indexDslConverter;
	private final ProjectionConverter<F, F> indexProjectionConverter;
	private final DslConverter<?, F> mappingDslConverter;
	private final ProjectionConverter<F, ?> mappingProjectionConverter;
	private final DslConverter<?, F> parseConverter;
	private final ProjectionConverter<F, ?> formatConverter;

	private final boolean searchable;
	private final boolean sortable;
	private final boolean projectable;
	private final boolean aggregable;
	private final boolean multivaluable;
	private final Set<SearchHighlighterType> allowedHighlighterTypes;

	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	protected AbstractIndexValueFieldType(Builder<SC, N, F> builder) {
		super( builder );
		this.valueClass = builder.valueClass;
		this.indexDslConverter = builder.indexDslConverter;
		this.indexProjectionConverter = builder.indexProjectionConverter;
		this.mappingDslConverter = builder.mappingDslConverter != null ? builder.mappingDslConverter : indexDslConverter;
		this.mappingProjectionConverter =
				builder.mappingProjectionConverter != null ? builder.mappingProjectionConverter : indexProjectionConverter;
		this.parseConverter = builder.parser != null ? builder.parser : indexDslConverter;
		this.formatConverter = builder.formatter != null ? builder.formatter : indexProjectionConverter;
		this.searchable = builder.searchable;
		this.sortable = builder.sortable;
		this.projectable = builder.projectable;
		this.aggregable = builder.aggregable;
		this.multivaluable = builder.multivaluable;
		this.allowedHighlighterTypes = Collections.unmodifiableSet( builder.allowedHighlighterTypes );
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
				+ ", traits=" + traits()
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
	public boolean multivaluable() {
		return multivaluable;
	}

	@Override
	public final Class<?> dslArgumentClass() {
		return mappingDslConverter.valueType();
	}

	@Override
	public final DslConverter<?, F> mappingDslConverter() {
		return mappingDslConverter;
	}

	@Override
	public DslConverter<?, F> parserDslConverter() {
		return parseConverter;
	}

	@Override
	public ProjectionConverter<F, ?> formatterProjectionConverter() {
		return formatConverter;
	}

	@Override
	public final DslConverter<F, F> indexDslConverter() {
		return indexDslConverter;
	}

	@Override
	public final Class<?> projectedValueClass() {
		return mappingProjectionConverter.valueType();
	}

	@Override
	public final ProjectionConverter<F, ?> mappingProjectionConverter() {
		return mappingProjectionConverter;
	}

	@Override
	public final ProjectionConverter<F, F> indexProjectionConverter() {
		return indexProjectionConverter;
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

	@Override
	public boolean highlighterTypeSupported(SearchHighlighterType type) {
		return allowedHighlighterTypes.contains( type );
	}

	public abstract static class Builder<
			SC extends SearchIndexScope<?>,
			N extends SearchIndexValueFieldContext<SC>,
			F>
			extends AbstractIndexNodeType.Builder<SC, N> {

		private final Class<F> valueClass;
		private final DslConverter<F, F> indexDslConverter;
		private final ProjectionConverter<F, F> indexProjectionConverter;

		private DslConverter<?, F> mappingDslConverter;
		private ProjectionConverter<F, ?> mappingProjectionConverter;
		private DslConverter<?, F> parser;
		private ProjectionConverter<F, ?> formatter;

		private boolean searchable;
		private boolean sortable;
		private boolean projectable;
		private boolean aggregable;
		private boolean multivaluable = true;
		private Set<SearchHighlighterType> allowedHighlighterTypes = Collections.emptySet();

		private String analyzerName;
		private String searchAnalyzerName;
		private String normalizerName;

		public Builder(Class<F> valueClass) {
			this.valueClass = valueClass;
			this.indexDslConverter = DslConverter.passThrough( valueClass );
			this.indexProjectionConverter = ProjectionConverter.passThrough( valueClass );
		}

		public final Class<F> valueClass() {
			return valueClass;
		}

		public final <V> void dslConverter(Class<V> valueType, ToDocumentValueConverter<V, ? extends F> toIndexConverter) {
			this.mappingDslConverter = new DslConverter<>( valueType, toIndexConverter );
		}

		public final <V> void projectionConverter(Class<V> valueType,
				FromDocumentValueConverter<? super F, V> fromIndexConverter) {
			this.mappingProjectionConverter = new ProjectionConverter<>( valueType, fromIndexConverter );
		}

		public final void parser(ToDocumentValueConverter<String, ? extends F> parser) {
			this.parser = new DslConverter<>( String.class, parser );
		}

		public final void formatter(FromDocumentValueConverter<? super F, String> formatter) {
			this.formatter = new ProjectionConverter<>( String.class, formatter );
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

		public final void multivaluable(boolean multivaluable) {
			this.multivaluable = multivaluable;
		}

		public final void allowedHighlighterTypes(Set<SearchHighlighterType> allowedHighlighterTypes) {
			this.allowedHighlighterTypes = allowedHighlighterTypes;
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
