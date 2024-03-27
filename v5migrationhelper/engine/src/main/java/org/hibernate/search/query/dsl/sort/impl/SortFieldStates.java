/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.spi.LuceneMigrationUtils;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.SortOrderStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.util.common.SearchException;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;

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

	private final SearchSortFactory factory;
	private final CompositeSortComponentsStep<?> delegate;

	private Type currentType;
	private String currentName;
	private SortOrder currentOrder;
	private Object currentMissingValue;
	private SortField currentSortFieldNativeSortDescription;
	private Coordinates coordinates;
	private Double currentLatitude;
	private Double currentLongitude;

	public SortFieldStates(QueryBuildingContext queryContext) {
		factory = queryContext.getScope().sort();
		delegate = factory.composite();
	}

	public void setCurrentType(Type currentType) {
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

	public void setDesc() {
		this.currentOrder = SortOrder.DESC;
	}

	public void setCurrentSortFieldNativeSortDescription(SortField currentSortField) {
		this.currentSortFieldNativeSortDescription = currentSortField;
	}

	public void closeSortField() {
		SearchSort sort;
		if ( currentSortFieldNativeSortDescription != null ) {
			sort = factory.extension( LuceneExtension.get() )
					.fromLuceneSortField( currentSortFieldNativeSortDescription ).toSort();
		}
		else if ( currentType == Type.SCORE ) {
			ScoreSortOptionsStep<?> optionsStep = factory.score();
			applyOrder( optionsStep );
			sort = optionsStep.toSort();
		}
		else if ( currentType == Type.DOC ) {
			sort = factory.indexOrder().toSort();
		}
		else if ( coordinates != null || currentLatitude != null ) {
			if ( currentMissingValue != null ) {
				throw new SearchException( "Missing values substitutes are not supported for distance sorting yet" );
			}
			GeoPoint center;
			if ( coordinates != null ) {
				center = Coordinates.toGeoPoint( coordinates );
			}
			else {
				center = GeoPoint.of( currentLatitude, currentLongitude );
			}
			DistanceSortOptionsStep<?, ?> optionsStep = factory.distance( currentName, center );
			applyOrder( optionsStep );
			sort = optionsStep.toSort();
		}
		else {
			FieldSortOptionsStep<?, ?> optionsStep = factory.field( currentName );
			applyOrder( optionsStep );
			applyMissing( optionsStep );
			sort = optionsStep.toSort();
		}
		delegate.add( sort );
		reset();
	}

	private void applyOrder(SortOrderStep<?> step) {
		if ( currentOrder != null ) {
			step.order( currentOrder );
		}
	}

	private void applyMissing(FieldSortOptionsStep<?, ?> step) {
		if ( currentMissingValue == null ) {
			return;
		}
		if ( currentMissingValue == MISSING_VALUE_LAST ) {
			step.missing().last();
		}
		else if ( currentMissingValue == MISSING_VALUE_FIRST ) {
			step.missing().first();
		}
		else {
			step.missing().use( currentMissingValue );
		}
	}

	public Sort createSort() {
		return LuceneMigrationUtils.toLuceneSort( delegate.toSort() );
	}

	private void reset() {
		this.currentType = null;
		this.currentName = null;
		this.currentOrder = null;
		this.currentMissingValue = null;
		this.currentSortFieldNativeSortDescription = null;
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
}
