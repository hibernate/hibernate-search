/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.dsl.projection.DocumentReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.FieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ObjectProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionContainerContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;


public class SearchProjectionContainerContextImpl implements SearchProjectionContainerContext {

	private final SearchProjectionFactory<?> factory;

	public SearchProjectionContainerContextImpl(SearchProjectionFactory<?> factory) {
		this.factory = factory;
	}

	@Override
	public DocumentReferenceProjectionContext documentReference() {
		return new DocumentReferenceProjectionContextImpl( factory );
	}

	@Override
	public <T> FieldProjectionContext<T> field(String absoluteFieldPath, Class<T> clazz) {
		return new FieldProjectionContextImpl<>( factory, absoluteFieldPath, clazz );
	}

	@Override
	public ReferenceProjectionContext reference() {
		return new ReferenceProjectionContextImpl( factory );
	}

	@Override
	public ObjectProjectionContext object() {
		return new ObjectProjectionContextImpl( factory );
	}

}
