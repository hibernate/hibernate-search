/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
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

	private static final Map<SortField.Type, Object> SCALAR_MINIMUMS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Object> SCALAR_MAXIMUMS = new EnumMap<>( SortField.Type.class );
	static {
		initScalarMinMax( SortField.Type.DOUBLE, Double.MIN_VALUE, Double.MAX_VALUE );
		initScalarMinMax( SortField.Type.FLOAT, Float.MIN_VALUE, Float.MAX_VALUE );
		initScalarMinMax( SortField.Type.LONG, Long.MIN_VALUE, Long.MAX_VALUE );
		initScalarMinMax( SortField.Type.INT, Integer.MIN_VALUE, Integer.MAX_VALUE );
	}

	private static void initScalarMinMax(Type type, double minValue, double maxValue) {
		SCALAR_MINIMUMS.put( type, minValue );
		SCALAR_MAXIMUMS.put( type, maxValue );
	}

	private enum SortOrder {
		ASC,
		DESC;
	}

	private final QueryBuildingContext queryContext;

	private SortField.Type currentType;
	private String currentName;
	private SortOrder currentOrder;
	private Object currentMissingValue;
	private SortField currentSortField;
	private Coordinates coordinates;
	private Double currentLatitude;
	private Double currentLongitude;

	public SortFieldStates(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
	}

	public void setCurrentType(SortField.Type currentType) {
		this.currentType = currentType;
	}

	public void setCurrentName(String fieldName) {
		this.currentName = fieldName;
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
		this.currentOrder = SortOrder.ASC;
	}

	public boolean isAsc() {
		return SortOrder.ASC.equals( currentOrder );
	}

	public void setDesc() {
		this.currentOrder = SortOrder.DESC;
	}

	public boolean isDesc() {
		return SortOrder.DESC.equals( currentOrder );
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
			sortField = new SortField( null, SortField.Type.SCORE, isAsc() );
		}
		else if ( currentType == SortField.Type.DOC ) {
			sortField = new SortField( null, SortField.Type.DOC, isDesc() );
		}
		else if ( coordinates != null ) {
			sortField = new DistanceSortField( coordinates, currentName, isAsc() );
			if ( hasMissingValue() ) {
				throw new AssertionFailure( "Missing values are not supported for distance sorting yet" );
			}
		}
		else if ( currentLatitude != null ) {
			sortField = new DistanceSortField( currentLatitude, currentLongitude, currentName, isAsc() );
			if ( hasMissingValue() ) {
				throw new AssertionFailure( "Missing values are not supported for distance sorting yet" );
			}
		}
		else {
			sortField = new SortField( currentName, currentType, isDesc() );
		}
		processMissingValue( sortField );
		sortFields.add( sortField );
		reset();
	}

	public void guessCurrentSortFieldType() {
		this.currentType = getCurrentSortFieldTypeGuess();
	}

	private SortField.Type getCurrentSortFieldTypeGuess() {
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
						return SortField.Type.DOUBLE;
					case FLOAT:
						return SortField.Type.FLOAT;
					case LONG:
						return SortField.Type.LONG;
					case INTEGER:
						return SortField.Type.INT;
					case UNKNOWN:
						break; // Fail below
				}
				break;
			case BASIC:
				// TODO here I assume it will be a String, is that correct?
				// TODO what about SortField.Type.String vs SortField.Type.StringVal
				return SortField.Type.STRING;
		}

		throw new SearchException( "Cannot guess the field type" );
	}

	private void processMissingValue(SortField sortField) {
		if ( currentMissingValue != null && sortField.getType() != null ) {
			if ( sortField.getType() == SortField.Type.STRING || sortField.getType() == SortField.Type.STRING_VAL ) {
				if ( currentMissingValue == MISSING_VALUE_LAST ) {
					sortField.setMissingValue( SortField.STRING_LAST );
				}
				else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
					sortField.setMissingValue( SortField.STRING_FIRST );
				}
				else {
					throw new SearchException( "Unsupported 'use(Object)' for the field type: " + currentType + "."
							+ " Only 'sortFirst()' and 'sortLast()' are supported." );
				}
			}
			else {
				boolean reverse = sortField.getReverse();
				if ( currentMissingValue == MISSING_VALUE_FIRST && !reverse
						|| currentMissingValue == MISSING_VALUE_LAST && reverse ) {
					Object min = SCALAR_MINIMUMS.get( sortField.getType() );
					if ( min != null ) {
						sortField.setMissingValue( min );
					}
					else {
						throw new SearchException( "Unsupported 'sortFirst()'/'sortLast()' for the field type: " + currentType + "."
								+ " Only 'use(Object)' is supported.");
					}
				}
				else if ( currentMissingValue == MISSING_VALUE_LAST && !reverse
						|| currentMissingValue == MISSING_VALUE_FIRST && reverse ) {
					Object max = SCALAR_MAXIMUMS.get( sortField.getType() );
					if ( max != null ) {
						sortField.setMissingValue( max );
					}
					else {
						throw new SearchException( "Unsupported 'sortFirst()'/'sortLast()' for the field type: " + currentType + "."
								+ " Only 'use(Object)' is supported.");
					}
				}
				else {
					/*
					 * Field bridge cannot be used for non-string sort values, since the field bridge
					 * only provides a String and the SortField only accepts a value of the
					 * actual sort type (Long, Double, ...).
					 * That's why we don't call useFieldBridgeIfNecessary() here.
					 */
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
		this.currentOrder = null;
		this.currentMissingValue = null;
		this.currentSortField = null;
		this.coordinates = null;
		this.currentLatitude = null;
		this.currentLongitude = null;
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
}
