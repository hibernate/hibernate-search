/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.util.common.reporting.EventContext;

public final class IndexIdentifier implements SearchIndexIdentifierContext {

	private final DslConverter<?, String> dslConverter;
	private final DslConverter<?, String> parser;
	private final ProjectionConverter<String, ?> projectionConverter;

	public IndexIdentifier(DslConverter<?, String> dslConverter, DslConverter<?, String> parser,
			ProjectionConverter<String, ?> projectionConverter) {
		this.dslConverter = dslConverter != null ? dslConverter : RAW_DSL_CONVERTER;
		this.parser = parser != null ? parser : RAW_DSL_CONVERTER;
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
	public DslConverter<?, String> parser() {
		return parser;
	}

	@Override
	public ProjectionConverter<String, ?> projectionConverter() {
		return projectionConverter;
	}

}
