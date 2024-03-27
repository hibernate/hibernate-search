/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class MarkerBindingContextImpl extends AbstractBindingContext
		implements MarkerBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			throw log.missingMarkerForBinder( binder );
		}

		return marker;
	}
}
