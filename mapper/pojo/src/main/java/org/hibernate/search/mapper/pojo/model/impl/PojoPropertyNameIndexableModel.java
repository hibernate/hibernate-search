/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.ReadableProperty;


/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNameIndexableModel implements IndexableModel {

	private final PojoIntrospector introspector;

	private final PojoIndexableReference<?> parent;

	private final String relativeName;

	public PojoPropertyNameIndexableModel(PojoIntrospector introspector, PojoIndexableReference<?> parent,
			String relativeName) {
		this.introspector = introspector;
		this.parent = parent;
		this.relativeName = relativeName;
	}

	@Override
	public <T> IndexableReference<T> asReference(Class<T> type) {
		ReadableProperty propertyReference = introspector.findReadableProperty( parent.getType(), relativeName, type );
		return new PojoPropertyIndexableReference<>( parent, propertyReference );
	}

	@Override
	public PojoIndexableReference<?> asReference() {
		ReadableProperty propertyReference = introspector.findReadableProperty( parent.getType(), relativeName );
		return new PojoPropertyIndexableReference<>( parent, propertyReference );
	}

	@Override
	public IndexableModel property(String relativeName) {
		return new PojoPropertyNameIndexableModel( introspector, asReference(), relativeName );
	}
}
