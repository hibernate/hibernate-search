/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about an identifier targeted by search.
 * <p>
 * This is used in predicates, projections, sorts, ...
 */
public interface SearchIndexIdentifierContext extends EventContextProvider {

	StringDocumentIdentifierValueConverter RAW_ID_CONVERTER = new StringDocumentIdentifierValueConverter();

	EventContext relativeEventContext();

	default DocumentIdentifierValueConverter<?> dslConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return RAW_ID_CONVERTER;
			case YES:
			default:
				return dslConverter();
		}
	}

	DocumentIdentifierValueConverter<?> dslConverter();

}
