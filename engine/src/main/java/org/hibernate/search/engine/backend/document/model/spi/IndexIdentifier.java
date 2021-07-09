/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.util.common.reporting.EventContext;

public final class IndexIdentifier implements SearchIndexIdentifierContext {

	private final DocumentIdentifierValueConverter<?> dslConverter;

	public IndexIdentifier(DocumentIdentifierValueConverter<?> dslConverter) {
		this.dslConverter = dslConverter != null ? dslConverter : new StringDocumentIdentifierValueConverter();
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
	public DocumentIdentifierValueConverter<?> dslConverter() {
		return dslConverter;
	}
}
