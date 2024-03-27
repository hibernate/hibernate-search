/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.util.common.reporting.EventContext;

public final class IndexIdentifier implements SearchIndexIdentifierContext {

	private final DslConverter<?, String> dslConverter;
	private final ProjectionConverter<String, ?> projectionConverter;

	public IndexIdentifier(DslConverter<?, String> dslConverter, ProjectionConverter<String, ?> projectionConverter) {
		this.dslConverter = dslConverter != null ? dslConverter : RAW_DSL_CONVERTER;
		this.projectionConverter = projectionConverter != null ? projectionConverter : RAW_PROJECTION_CONVERTER;
	}

	@Override
	public EventContext eventContext() {
		return relativeEventContext();
	}

	@Override
	public EventContext relativeEventContext() {
		return EventContexts.indexSchemaIdentifier();
	}

	@Override
	public DslConverter<?, String> dslConverter() {
		return dslConverter;
	}

	@Override
	public ProjectionConverter<String, ?> projectionConverter() {
		return projectionConverter;
	}

}
