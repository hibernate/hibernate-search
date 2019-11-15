/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchFailingIdCompatibilityChecker implements ElasticsearchCompatibilityChecker {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ToDocumentIdentifierValueConverter<?> identifierValueConverter1;
	private final ToDocumentIdentifierValueConverter<?> identifierValueConverter2;
	private final EventContext eventContext;

	ElasticsearchFailingIdCompatibilityChecker(ToDocumentIdentifierValueConverter<?> identifierValueConverter1,
			ToDocumentIdentifierValueConverter<?> identifierValueConverter2,
			EventContext eventContext) {
		this.identifierValueConverter1 = identifierValueConverter1;
		this.identifierValueConverter2 = identifierValueConverter2;
		this.eventContext = eventContext;
	}

	@Override
	public void failIfNotCompatible() {
		throw log.conflictingIdentifierTypesForPredicate(
				identifierValueConverter1, identifierValueConverter2, eventContext
		);
	}

	@Override
	public ElasticsearchCompatibilityChecker combine(ElasticsearchCompatibilityChecker other) {
		// failing + any = failing
		return this;
	}
}
