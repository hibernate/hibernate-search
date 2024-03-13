/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Information about the type of a value (non-object) field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 * @param <N> The type representing the targeted index node.
 * @param <F> The indexed field value type.
 */
public interface SearchIndexValueFieldTypeContext<
		SC extends SearchIndexScope<?>,
		N,
		F>
		extends SearchIndexNodeTypeContext<SC, N> {

	Class<F> valueClass();

	DslConverter<?, F> dslConverter();

	@Incubating
	DslConverter<?, F> parser();

	DslConverter<F, F> rawDslConverter();

	default DslConverter<?, F> dslConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawDslConverter();
			case PARSE:
				return parser();
			case YES:
			default:
				return dslConverter();
		}
	}

	ProjectionConverter<F, ?> projectionConverter();

	ProjectionConverter<F, F> rawProjectionConverter();

	default ProjectionConverter<F, ?> projectionConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawProjectionConverter();
			case PARSE:
				throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).parseConverterNotAllowed(
						ValueConvert.class.getSimpleName(),
						ValueConvert.PARSE,
						List.of( ValueConvert.YES, ValueConvert.NO )
				);
			case YES:
			default:
				return projectionConverter();
		}
	}

	boolean highlighterTypeSupported(SearchHighlighterType type);
}
