/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.spi.FunctionBridge;
import org.hibernate.search.mapper.pojo.model.spi.Indexable;
import org.hibernate.search.mapper.pojo.model.spi.IndexableReference;


/**
 * @author Yoann Rodiere
 */
public class FunctionBridgeValueProcessor<T, R> implements ValueProcessor {

	private final FunctionBridge<T, R> bridge;
	private final IndexableReference<? extends T> indexableReference;
	private final IndexFieldReference<R> indexFieldReference;

	public FunctionBridgeValueProcessor(FunctionBridge<T, R> bridge,
			IndexableReference<? extends T> indexableReference,
			IndexFieldReference<R> indexFieldReference) {
		this.bridge = bridge;
		this.indexableReference = indexableReference;
		this.indexFieldReference = indexFieldReference;
	}

	@Override
	public void process(Indexable source, DocumentState target) {
		T indexableValue = source.get( indexableReference );
		R indexFieldValue = bridge.toDocument( indexableValue );
		indexFieldReference.add( target, indexFieldValue );
	}

	@Override
	public void close() {
		bridge.close();
	}

}
