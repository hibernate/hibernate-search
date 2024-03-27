/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

/**
 * A POJO processor responsible for transferring data from the POJO to a document to index.
 *
 * @param <T> The processed type
 */
public abstract class PojoIndexingProcessor<T> implements AutoCloseable, ToStringTreeAppendable {

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void close() {
	}

	public abstract void process(DocumentElement target, T source, PojoIndexingProcessorRootContext context);

	public static <T> PojoIndexingProcessor<T> noOp() {
		return NoOpPojoIndexingProcessor.get();
	}

}
