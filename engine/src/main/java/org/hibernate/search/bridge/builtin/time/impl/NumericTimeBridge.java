/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * A bridge that converts a class in the jva.time package to a numeric field.
 *
 * @author Davide D'Alto
 */
public interface NumericTimeBridge {

	/**
	 * Define the numeric encoding to use for the brdige.
	 *
	 * @return the encoding to use for this bridge
	 */
	NumericEncodingType getEncodingType();
}
