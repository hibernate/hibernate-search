/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor;
import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.DistanceSortField;

/**
 * Holds the list of @{link SortField}s as well as the state of the one being constructed.
 * Use {@link #closeSortField()} to add the current {@code SortField} to the list
 * of created sort fields.
 * Use {@link #createSort()} to return Lucene's sort object.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class SortFieldStates {

	private static final Object MISSING_VALUE_LAST = new Object();
	private static final Object MISSING_VALUE_FIRST = new Object();

	private final QueryBuildingContext queryContext;

	private SortField.Type currentType;
	private String currentName;
	private Boolean currentReverse;
	private Object currentMissingValue;
	private SortField currentSortField;
	private Coordinates coordinates;
	private Double currentLatitude;
	private Double currentLongitude;
	private Boolean currentIgnoreFieldBridge;

	public SortFieldStates(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
	}

	public void setCurrentType(SortField.Type currentType) {
		this.currentType = currentType;
	}

	public void setCurrentName(String currentName) {
		this.currentName = currentName;
	}

	public void setCurrentMissingValue(Object currentMissingValue) {
		this.currentMissingValue = currentMissingValue;
	}

	public void setCurrentMissingValueLast() {
		this.currentMissingValue = MISSING_VALUE_LAST;
	}

	public void setCurrentMissingValueFirst() {
		this.currentMissingValue = MISSING_VALUE_FIRST;
	}

	public void setAsc() {
		if ( currentType == SortField.Type.SCORE ) {
			currentReverse = Boolean.TRUE;
		}
		else if ( coordinates != null || currentLatitude != null ) {
			currentReverse = Boolean.TRUE;
		}
		else {
			currentReverse = Boolean.FALSE;
		}
	}

	public void setDesc() {
		if ( currentType == SortField.Type.SCORE ) {
			currentReverse = Boolean.FALSE;
		}
		else if ( coordinates != null || currentLatitude != null ) {
			currentReverse = Boolean.FALSE;
		}
		else {
			currentReverse = Boolean.TRUE;
		}
	}

	public List<SortField> sortFields = new ArrayList<>( 3 );

	public void setCurrentSortField(SortField currentSortField) {
		this.currentSortField = currentSortField;
	}

	public void closeSortField() {
		SortField sortField;
		if ( currentSortField != null ) {
			sortField = currentSortField;
		}
		else if ( currentType == SortField.Type.SCORE ) {
			sortField = Boolean.TRUE == currentReverse ? new SortField( null, SortField.Type.SCORE, true ) : SortField.FIELD_SCORE;
		}
		else if ( currentType == SortField.Type.DOC ) {
			sortField = Boolean.TRUE == currentReverse ? new SortField( null, SortField.Type.DOC, true ) : SortField.FIELD_DOC;
		}
		else if ( coordinates != null ) {
			if ( currentReverse == Boolean.TRUE ) {
				sortField = new DistanceSortField( coordinates, currentName, true );
			}
			else {
				sortField = new DistanceSortField( coordinates, currentName );
			}
			if ( hasMissingValue() ) {
				throw new AssertionFailure( "Missing values are not supported for distance sorting yet" );
			}
		}
		else if ( currentLatitude != null ) {
			if ( currentReverse == Boolean.TRUE ) {
				sortField = new DistanceSortField( currentLatitude, currentLongitude, currentName, true );
			}
			else {
				sortField = new DistanceSortField( currentLatitude, currentLongitude, currentName );
			}
			if ( hasMissingValue() ) {
				throw new AssertionFailure( "Missing values are not supported for distance sorting yet" );
			}
		}
		else {
			// handle non spatial, non score and non doc id sorting
			processSortFieldType();
			sortField = new SortField( currentName, currentType, currentReverse );
		}
		processMissingValue( sortField );
		sortFields.add( sortField );
		reset();
	}

	private void processSortFieldType() {
		FieldDescriptor fieldDescriptor = queryContext.getFactory()
				.getIndexedTypeDescriptor( queryContext.getEntityType() )
				.getIndexedField( currentName );
		switch ( fieldDescriptor.getType() ) {
			case SPATIAL:
				throw new SearchException( "wrong field type mate, use .fromCoordinates and co" );
			case NUMERIC:
				NumericFieldSettingsDescriptor nfd = fieldDescriptor.as( NumericFieldSettingsDescriptor.class );
				switch ( nfd.encodingType() ) {
					case DOUBLE:
						currentType = SortField.Type.DOUBLE;
						break;
					case FLOAT:
						currentType = SortField.Type.FLOAT;
						break;
					case LONG:
						currentType = SortField.Type.LONG;
						break;
					case INTEGER:
						currentType = SortField.Type.INT;
						break;
				}
				break;
			case BASIC:
				// TODO here I assume it will be a String, is that correct?
				// TODO what about SortField.Type.String vs SortField.Type.StringVal
				currentType = SortField.Type.STRING;
				break;
		}
		if ( currentType == null ) {
			throw new SearchException( "Cannot guess the field type" );
		}
	}

	private void processMissingValue(SortField sortField) {
		if ( currentMissingValue != null && currentType != null ) {
			if ( currentType == SortField.Type.STRING || currentType == SortField.Type.STRING_VAL ) {
				if ( currentMissingValue == MISSING_VALUE_LAST ) {
					sortField.setMissingValue( SortField.STRING_LAST );
				}
				else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
					sortField.setMissingValue( SortField.STRING_FIRST );
				}
				else {
					sortField.setMissingValue( currentMissingValue );
				}
			}
			else if ( currentType == SortField.Type.DOUBLE ) {
				if ( currentMissingValue == MISSING_VALUE_LAST ) {
					sortField.setMissingValue( Double.MAX_VALUE );
				}
				else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
					sortField.setMissingValue( Double.MIN_VALUE );
				}
				else {
					sortField.setMissingValue( currentMissingValue );
				}
			}
			else if ( currentType == SortField.Type.FLOAT ) {
				if ( currentMissingValue == MISSING_VALUE_LAST ) {
					sortField.setMissingValue( Float.MAX_VALUE );
				}
				else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
					sortField.setMissingValue( Float.MIN_VALUE );
				}
				else {
					sortField.setMissingValue( currentMissingValue );
				}
			}
			else if ( currentType == SortField.Type.INT ) {
				if ( currentMissingValue == MISSING_VALUE_LAST ) {
					sortField.setMissingValue( Integer.MAX_VALUE );
				}
				else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
					sortField.setMissingValue( Integer.MIN_VALUE );
				}
				else {
					sortField.setMissingValue( currentMissingValue );
				}
			}
			else if ( currentType == SortField.Type.LONG ) {
				if ( currentMissingValue == MISSING_VALUE_LAST ) {
					sortField.setMissingValue( Long.MAX_VALUE );
				}
				else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
					sortField.setMissingValue( Long.MIN_VALUE );
				}
				else {
					sortField.setMissingValue( currentMissingValue );
				}
			}
		}
	}

	public Sort createSort() {
		return new Sort( sortFields.toArray( new SortField[ sortFields.size() ] ) );
	}

	private boolean hasMissingValue() {
		return currentMissingValue != null;
	}

	private void reset() {
		this.currentType = null;
		this.currentName = null;
		this.currentReverse = null;
		this.currentMissingValue = null;
		this.currentSortField = null;
		this.coordinates = null;
		this.currentLatitude = null;
		this.currentLongitude = null;
		this.currentIgnoreFieldBridge = null;
	}

	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}

	public void setCurrentLatitude(double latitude) {
		this.currentLatitude = latitude;
	}

	public void setCurrentLongitude(double longitude) {
		this.currentLongitude = longitude;
	}

	public void setCurrentSortField(String sortField) {
		throw new AssertionFailure( "What to do for ES byNative(String)?" );
	}

	public void setCurrentSpatialQuery(Query query) {
		//TODO implement by:
		// - unwrapping the original query
		// - extract the field name
		// - extract the coordinates or lat/long from the DistanceFilter
		// - see ConnectedSpatialQueryBuilder
		throw new AssertionFailure( "byDistanceFromSpatialQuery not implemented yet" );
	}

	public void setCurrentIgnoreFieldBridge() {
		this.currentIgnoreFieldBridge = Boolean.TRUE;
	}
}
