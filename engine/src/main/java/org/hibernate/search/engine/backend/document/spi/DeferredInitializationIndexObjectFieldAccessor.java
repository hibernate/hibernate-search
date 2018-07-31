/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;


/**
 * @author Yoann Rodiere
 */
class DeferredInitializationIndexObjectFieldAccessor implements IndexObjectFieldAccessor {

	private IndexObjectFieldAccessor delegate;

	void initialize(IndexObjectFieldAccessor delegate) {
		this.delegate = delegate;
	}

	@Override
	public DocumentElement add(DocumentElement target) {
		if ( delegate != null ) {
			return delegate.add( target );
		}
		else {
			/*
			 * The object was filtered out - ignore it.
			 * Note the actual type of the returned instance should not matter,
			 * since child accessors should have been filtered out too and should be no-ops.
			 */
			return NoOpDocumentElement.INSTANCE;
		}
	}

	@Override
	public void addMissing(DocumentElement target) {
		if ( delegate != null ) {
			delegate.addMissing( target );
		}
		// else: The object was filtered out - ignore it
	}
}
