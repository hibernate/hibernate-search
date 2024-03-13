/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about an identifier targeted by search.
 * <p>
 * This is used in predicates, projections, sorts, ...
 */
public interface SearchIndexIdentifierContext extends EventContextProvider {

	DslConverter<String, String> RAW_DSL_CONVERTER = DslConverter.passThrough( String.class );
	ProjectionConverter<String, String> RAW_PROJECTION_CONVERTER = ProjectionConverter.passThrough( String.class );

	EventContext relativeEventContext();

	default DslConverter<?, String> dslConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return RAW_DSL_CONVERTER;
			case PARSE:
				return parser();
			case YES:
			default:
				return dslConverter();
		}
	}

	DslConverter<?, String> dslConverter();

	@Incubating
	DslConverter<?, String> parser();

	ProjectionConverter<String, ?> projectionConverter();

}
