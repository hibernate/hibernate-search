/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;

public final class MarkerBindingContextImpl extends AbstractBindingContext
		implements MarkerBindingContext {

	private Object marker;

	public MarkerBindingContextImpl(BeanResolver beanResolver, Map<String, Object> params) {
		super( beanResolver, params );
	}

	@Override
	public void marker(Object marker) {
		this.marker = marker;
	}

	public Object applyBinder(MarkerBinder binder) {
		// This call should set the partial binding
		binder.bind( this );
		if ( marker == null ) {
			throw MappingLog.INSTANCE.missingMarkerForBinder( binder );
		}

		return marker;
	}
}
