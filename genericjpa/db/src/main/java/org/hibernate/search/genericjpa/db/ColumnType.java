/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db;

import org.hibernate.search.genericjpa.exception.AssertionFailure;
import org.hibernate.search.genericjpa.exception.SearchException;

/**
 * Created by Martin on 21.07.2015.
 */
public enum ColumnType implements IdConverter {
	STRING {
		@Override
		public Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
			if ( columnTypes[0] != this ) {
				throw new AssertionFailure( "passed idType was not equal to this" );
			}
			if ( values.length != 1 && fieldNames.length != 1 ) {
				throw new AssertionFailure( "values.length and fieldNames.length should be equal to 1" );
			}
			return String.valueOf( values[0] );
		}
	},
	INTEGER {
		@Override
		public Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
			if ( columnTypes[0] != this ) {
				throw new AssertionFailure( "passed idType was not equal to this" );
			}
			if ( values.length != 1 && fieldNames.length != 1 ) {
				throw new AssertionFailure( "values.length and fieldNames.length should be equal to 1" );
			}
			Object val = values[0];
			if ( val instanceof Number ) {
				return ((Number) val).intValue();
			}
			else {
				throw new SearchException( fieldNames[0] + " is no Number" );
			}
		}
	},
	LONG {
		@Override
		public Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
			if ( columnTypes[0] != this ) {
				throw new AssertionFailure( "passed idType was not equal to this" );
			}
			if ( values.length != 1 && fieldNames.length != 1 ) {
				throw new AssertionFailure( "values.length and fieldNames.length should be equal to 1" );
			}
			Object val = values[0];
			if ( val instanceof Number ) {
				return ((Number) val).longValue();
			}
			else {
				throw new SearchException( fieldNames[0] + " is no Number" );
			}
		}
	},
	CUSTOM {
		@Override
		public Object convert(
				Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
			throw new AssertionFailure( "custom types can not be used on their own" );
		}
	}
}
