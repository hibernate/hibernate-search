/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * A projection used whenever a given type has a one-way field bridge, which is forbidden.
 *
 * @author Yoann Rodiere
 */
class FailingOneWayFieldBridgeProjection extends FieldProjection {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final String absoluteName;
	private final Class<?> fieldBridgeClass;

	public FailingOneWayFieldBridgeProjection(String absoluteName, Class<?> fieldBridgeClass) {
		super();
		this.absoluteName = absoluteName;
		this.fieldBridgeClass = fieldBridgeClass;
	}

	@Override
	public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
		throw LOG.projectingFieldWithoutTwoWayFieldBridge( absoluteName, fieldBridgeClass );
	}
}