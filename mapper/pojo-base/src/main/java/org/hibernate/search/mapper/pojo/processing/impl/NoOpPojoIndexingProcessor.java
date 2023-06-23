/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

class NoOpPojoIndexingProcessor extends PojoIndexingProcessor<Object> {

	private static final NoOpPojoIndexingProcessor INSTANCE = new NoOpPojoIndexingProcessor();

	@SuppressWarnings("unchecked") // This instance works for any T
	public static <T> PojoIndexingProcessor<T> get() {
		return (PojoIndexingProcessor<T>) INSTANCE;
	}

	@Override
	public void process(DocumentElement target, Object source, PojoIndexingProcessorRootContext context) {
		// No-op
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "no op" );
	}
}
