/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldAccessor;


/**
 * @author Yoann Rodiere
 */
public class DeferredInitializationIndexFieldAccessor<T> implements IndexFieldAccessor<T> {

	private IndexFieldAccessor<T> delegate;

	public void initialize(IndexFieldAccessor<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void write(DocumentState state, T value) {
		if ( delegate != null ) {
			delegate.write( state, value );
		}
		// else: The field was filtered out - ignore it
	}

}
