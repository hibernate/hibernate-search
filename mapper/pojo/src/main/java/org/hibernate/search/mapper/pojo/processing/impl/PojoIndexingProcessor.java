/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A POJO processor responsible for transferring data from the POJO to a document to index.
 *
 * @param <T> The processed type
 */
public abstract class PojoIndexingProcessor<T> implements AutoCloseable, ToStringTreeAppendable {

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void close() {
	}

	public abstract void process(DocumentElement target, T source, AbstractPojoBackendSessionContext sessionContext);

	public static <T> PojoIndexingProcessor<T> noOp() {
		return NoOpPojoIndexingProcessor.get();
	}

}
