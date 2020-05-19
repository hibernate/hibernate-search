/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class MarkerBindingContextImpl extends AbstractBindingContext
		implements MarkerBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Object marker;

	public MarkerBindingContextImpl(BeanResolver beanResolver) {
		super( beanResolver );
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
