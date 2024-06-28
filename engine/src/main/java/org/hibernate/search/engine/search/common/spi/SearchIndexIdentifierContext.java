/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueModel;
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

	default DslConverter<?, String> dslConverter(ValueModel valueModel) {
		switch ( valueModel ) {
			case INDEX:
			case RAW: // TODO: HSEARCH-5187
				return RAW_DSL_CONVERTER;
			case STRING:
				return parser();
			case MAPPING:
			default:
				return dslConverter();
		}
	}

	DslConverter<?, String> dslConverter();

	@Incubating
	DslConverter<?, String> parser();

	ProjectionConverter<String, ?> projectionConverter();

}
