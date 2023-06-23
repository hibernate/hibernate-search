/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.search.common.spi.SearchIndexNodeTypeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractIndexNode<
		S extends AbstractIndexNode<S, SC, ?>,
		SC extends SearchIndexScope<?>,
		NT extends SearchIndexNodeTypeContext<SC, ? super S>>
		implements IndexNode<SC> {
	protected final NT type;

	public AbstractIndexNode(NT type) {
		this.type = type;
	}

	protected abstract S self();

	public final NT type() {
		return type;
	}

	@Override
	public final EventContext eventContext() {
		return relativeEventContext();
	}

	@Override
	public final <T> T queryElement(SearchQueryElementTypeKey<T> key, SC scope) {
		SearchQueryElementFactory<? extends T, ? super SC, ? super S> factory = type.queryElementFactory( key );
		return helper().queryElement( key, factory, scope, self() );
	}

	@Override
	public SearchException cannotUseQueryElement(SearchQueryElementTypeKey<?> key, String hint, Exception causeOrNull) {
		return helper().cannotUseQueryElement( key, self(), hint, causeOrNull );
	}

	abstract SearchIndexSchemaElementContextHelper helper();

}
