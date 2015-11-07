/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import org.hibernate.search.genericjpa.db.ColumnType;
import org.hibernate.search.genericjpa.db.IdConverter;

/**
 * Created by Martin on 21.07.2015.
 */
public class MultipleColumnsIdEntityIdConverter implements IdConverter {

	@Override
	public Object convert(
			Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
		ID ret = new ID();
		ret.setFirstId( (String) values[0] );
		ret.setSecondId( (String) values[1] );
		return ret;
	}
}
