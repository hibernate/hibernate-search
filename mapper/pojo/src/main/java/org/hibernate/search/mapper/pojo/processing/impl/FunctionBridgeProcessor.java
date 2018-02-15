/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;


/**
 * @author Yoann Rodiere
 */
public class FunctionBridgeProcessor<T, R> implements PojoNodeProcessor<T> {

	private final FunctionBridge<? super T, R> bridge;
	private final IndexFieldAccessor<R> indexFieldAccessor;

	public FunctionBridgeProcessor(FunctionBridge<? super T, R> bridge,
			IndexFieldAccessor<R> indexFieldAccessor) {
		this.bridge = bridge;
		this.indexFieldAccessor = indexFieldAccessor;
	}

	@Override
	public void process(DocumentElement target, T source) {
		R indexFieldValue = bridge.toIndexedValue( source );
		indexFieldAccessor.write( target, indexFieldValue );
	}

	@Override
	public void close() {
		bridge.close();
	}

}
