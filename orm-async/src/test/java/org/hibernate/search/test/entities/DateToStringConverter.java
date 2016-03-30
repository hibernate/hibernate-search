/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import java.sql.Date;

import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.IdConverter;

/**
 * @author Martin Braun
 */
public class DateToStringConverter implements IdConverter {

	@Override
	public Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
		Date date = Date.valueOf( (String) values[0] );
		return date;
	}

}
