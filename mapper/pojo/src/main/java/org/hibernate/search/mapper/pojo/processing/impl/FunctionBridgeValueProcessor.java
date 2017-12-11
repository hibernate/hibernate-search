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
import org.hibernate.search.mapper.pojo.model.spi.BridgedElement;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementReader;


/**
 * @author Yoann Rodiere
 */
public class FunctionBridgeValueProcessor<T, R> implements ValueProcessor {

	private final FunctionBridge<T, R> bridge;
	private final BridgedElementReader<? extends T> bridgedElementReader;
	private final IndexFieldReference<R> indexFieldReference;

	public FunctionBridgeValueProcessor(FunctionBridge<T, R> bridge,
			BridgedElementReader<? extends T> bridgedElementReader,
			IndexFieldReference<R> indexFieldReference) {
		this.bridge = bridge;
		this.bridgedElementReader = bridgedElementReader;
		this.indexFieldReference = indexFieldReference;
	}

	@Override
	public void process(DocumentState target, BridgedElement source) {
		T bridgedElement = bridgedElementReader.read( source );
		R indexFieldValue = bridge.toDocument( bridgedElement );
		indexFieldReference.add( target, indexFieldValue );
	}

	@Override
	public void close() {
		bridge.close();
	}

}
