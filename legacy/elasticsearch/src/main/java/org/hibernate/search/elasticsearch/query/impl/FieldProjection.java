/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.util.regex.Pattern;

import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

abstract class FieldProjection {

	private static final Pattern DOT = Pattern.compile( "\\." );

	/**
	 * Returns the value of the projected field as retrieved from the ES result and converted using the corresponding
	 * field bridge. In case this bridge is not a 2-way bridge, the unconverted value will be returned.
	 */
	public abstract Object convertHit(JsonObject hit, ConversionContext conversionContext);

	protected final JsonElement extractFieldValue(JsonObject parent, String projectedField) {
		String field = projectedField;

		if ( FieldHelper.isEmbeddedField( projectedField ) ) {
			String[] parts = DOT.split( projectedField );
			field = parts[parts.length - 1];

			for ( int i = 0; i < parts.length - 1; i++ ) {
				JsonElement newParent = parent.get( parts[i] );
				if ( newParent == null ) {
					return null;
				}

				parent = newParent.getAsJsonObject();
			}
		}

		return parent.getAsJsonObject().get( field );
	}

}