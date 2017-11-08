/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;


/**
 * @author Yoann Rodiere
 */
public class DeferredInitializationIndexFieldReference<T> implements IndexFieldReference<T> {

	private IndexFieldReference<T> delegate;

	public void initialize(IndexFieldReference<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void add(DocumentState state, T value) {
		if ( delegate != null ) {
			delegate.add( state, value );
		}
		// else: The field was filtered out - ignore it
	}

}
