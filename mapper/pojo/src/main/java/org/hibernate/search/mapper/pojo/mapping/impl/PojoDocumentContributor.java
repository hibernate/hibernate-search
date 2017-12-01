/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessor;

class PojoDocumentContributor<D extends DocumentState, E> implements DocumentContributor<D> {

	private final PojoTypeNodeProcessor processor;

	private final Supplier<E> entitySupplier;

	PojoDocumentContributor(PojoTypeNodeProcessor processor, Supplier<E> entitySupplier) {
		this.processor = processor;
		this.entitySupplier = entitySupplier;
	}

	@Override
	public void contribute(D state) {
		processor.process( entitySupplier.get(), state );
	}
}
