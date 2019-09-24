/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;


class ConfiguredIndexSchemaNestingContext implements IndexSchemaNestingContext {

	private static final ConfiguredIndexSchemaNestingContext ROOT =
			new ConfiguredIndexSchemaNestingContext( IndexSchemaFilter.root(), "", "" );

	public static ConfiguredIndexSchemaNestingContext root() {
		return ROOT;
	}

	private final IndexSchemaFilter filter;
	private final String prefixFromFilter;
	private final String unconsumedPrefix;

	private ConfiguredIndexSchemaNestingContext(IndexSchemaFilter filter, String prefixFromFilter,
			String unconsumedPrefix) {
		this.filter = filter;
		this.prefixFromFilter = prefixFromFilter;
		this.unconsumedPrefix = unconsumedPrefix;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "filter=" ).append( filter )
				.append( ",prefixFromFilter=" ).append( prefixFromFilter )
				.append( ",unconsumedPrefix=" ).append( unconsumedPrefix )
				.append( "]" )
				.toString();
	}

	@Override
	public <T> T nest(String relativeFieldName, Function<String, T> nestedElementFactoryIfIncluded,
			Function<String, T> nestedElementFactoryIfExcluded) {
		String nameRelativeToFilter = prefixFromFilter + relativeFieldName;
		String prefixedRelativeName = unconsumedPrefix + relativeFieldName;
		if ( filter.isPathIncluded( nameRelativeToFilter ) ) {
			return nestedElementFactoryIfIncluded.apply( prefixedRelativeName );
		}
		else {
			return nestedElementFactoryIfExcluded.apply( prefixedRelativeName );
		}
	}

	@Override
	public <T> T nest(String relativeFieldName,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfIncluded,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfExcluded) {
		String nameRelativeToFilter = prefixFromFilter + relativeFieldName;
		String prefixedRelativeName = unconsumedPrefix + relativeFieldName;
		if ( filter.isPathIncluded( nameRelativeToFilter ) ) {
			ConfiguredIndexSchemaNestingContext nestedFilter =
					new ConfiguredIndexSchemaNestingContext( filter, nameRelativeToFilter + ".", "" );
			return nestedElementFactoryIfIncluded.apply( prefixedRelativeName, nestedFilter );
		}
		else {
			return nestedElementFactoryIfExcluded.apply( prefixedRelativeName, IndexSchemaNestingContext.excludeAll() );
		}
	}

	public <T> Optional<T> addIndexedEmbeddedIfIncluded(
			MappableTypeModel parentTypeModel, String relativePrefix,
			Integer nestedMaxDepth, Set<String> nestedPathFilters,
			NestedContextBuilder<T> contextBuilder) {
		IndexSchemaFilter composedFilter = filter.compose(
				parentTypeModel, relativePrefix, nestedMaxDepth, nestedPathFilters
		);
		if ( !composedFilter.isEveryPathExcluded() ) {
			String prefixToParse = unconsumedPrefix + relativePrefix;
			int afterPreviousDotIndex = 0;
			int nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			while ( nextDotIndex >= 0 ) {
				String objectName = prefixToParse.substring( afterPreviousDotIndex, nextDotIndex );
				contextBuilder.appendObject( objectName );

				// Make sure to mark the paths as encountered in the filter
				String objectNameRelativeToFilter = prefixToParse.substring( 0, nextDotIndex );
				// We only use isPathIncluded for its side effect: it marks the path as encountered
				filter.isPathIncluded( objectNameRelativeToFilter );

				afterPreviousDotIndex = nextDotIndex + 1;
				nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			}
			String unconsumedPrefix = prefixToParse.substring( afterPreviousDotIndex );

			ConfiguredIndexSchemaNestingContext nestedContext =
					new ConfiguredIndexSchemaNestingContext( composedFilter, "", unconsumedPrefix );
			return Optional.of( contextBuilder.build( nestedContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	public Set<String> getUselessIncludePaths() {
		Set<String> includePaths = filter.getLocalExplicitlyIncludedPaths();
		Map<String, Boolean> encounteredFieldPaths = filter.getEncounteredFieldPaths();
		Set<String> uselessIncludePaths = new LinkedHashSet<>();
		for ( String path : includePaths ) {
			Boolean included = encounteredFieldPaths.get( path );
			if ( included == null /* not encountered */ || !included ) {
				// An "includePaths" filter that does not result in inclusion is useless
				uselessIncludePaths.add( path );
			}
		}
		return uselessIncludePaths;
	}

	public Set<String> getEncounteredFieldPaths() {
		return filter.getEncounteredFieldPaths().keySet();
	}

	public interface NestedContextBuilder<T> {

		void appendObject(String objectName);

		T build(ConfiguredIndexSchemaNestingContext nestingContext);

	}
}