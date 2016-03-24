/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.IdConverter;

/**
 * @author Martin Braun
 */
public class CustomIdConverter implements IdConverter, TwoWayStringBridge {

	@Override
	public Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
		return new CustomIdClass( (Integer) values[0], (Integer) values[1] );
	}

	@Override
	public Object stringToObject(String stringValue) {
		String[] split = stringValue.split( "_" );
		return new CustomIdClass( Integer.parseInt( split[0] ), Integer.parseInt( split[1] ) );
	}

	@Override
	public String objectToString(Object object) {
		CustomIdClass val = (CustomIdClass) object;
		return val.getId() + "_" + val.getId2();
	}
}
