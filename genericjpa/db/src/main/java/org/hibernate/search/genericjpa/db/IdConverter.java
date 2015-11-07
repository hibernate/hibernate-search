/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db;

/**
 * Created by Martin on 20.07.2015.
 */
public interface IdConverter {

	/**
	 * @param values the values from the database
	 * @param fieldNames the fieldNames corresponding to the values
	 * @param columnTypes the columnTypes corresponding to the values
	 */
	Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes);

}
