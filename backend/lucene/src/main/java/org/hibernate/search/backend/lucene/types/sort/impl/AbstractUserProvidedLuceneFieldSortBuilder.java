/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import static org.apache.lucene.search.SortField.Type.FLOAT;
import static org.apache.lucene.search.SortField.Type.INT;
import static org.apache.lucene.search.SortField.Type.LONG;
import org.apache.lucene.search.join.ScoreMode;
import org.hibernate.search.backend.lucene.ValueSortField;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.dsl.impl.AbstractLuceneUserProvidedIndexFieldTypes;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneFieldComparatorSource;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneNumericFieldComparatorSource;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneTextFieldComparatorSource;

/**
 *
 * @author Waldemar Kłaczyński
 */
public abstract class AbstractUserProvidedLuceneFieldSortBuilder extends AbstractLuceneUserProvidedIndexFieldTypes {

	public AbstractUserProvidedLuceneFieldSortBuilder() {
		super( false, true, false, false );
	}

	protected void collectSortField(LuceneSearchSortCollector collector, SortField sortedField, String nestedDocumentPath) {
		SortField resolved = sortedField;

		String absoluteFieldPath = sortedField.getField();
		SortField.Type type = sortedField.getType();
		Object missingValue = sortedField.getMissingValue();
		boolean reverse = sortedField.getReverse();

		MultiValueMode sortMode = MultiValueMode.MIN;
		Query filter = null;
		Object nullAsValue = null;

		LuceneFieldComparatorSource fieldComparatorSource = null;

		if ( sortedField instanceof ValueSortField ) {
			ValueSortField valueSortField = (ValueSortField) sortedField;
			if ( valueSortField.getMode() != null ) {
				sortMode = getMultiValueMode( valueSortField.getMode() );
			}
			filter = valueSortField.getFilter();
			nullAsValue = valueSortField.getNullAsValue();
		}

		if ( isNumericSortTypes( type ) ) {
			AbstractLuceneNumericFieldCodec codec = getNumericCodec( type, nullAsValue );
			fieldComparatorSource = new LuceneNumericFieldComparatorSource(
				nestedDocumentPath, codec.getDomain(), (Number) getEffectiveMissingValue( codec, missingValue, reverse ), sortMode, filter );
			resolved = new SortField( absoluteFieldPath, fieldComparatorSource, reverse );
		}
		else if ( isTextSortTypes( type ) ) {
			fieldComparatorSource = new LuceneTextFieldComparatorSource( nestedDocumentPath, missingValue, sortMode, filter );
			resolved = new SortField( absoluteFieldPath, fieldComparatorSource, reverse );
		}

		collector.collectSortField( resolved, (nestedDocumentPath != null) ? fieldComparatorSource : null );

	}

	public boolean isNumericSortTypes(SortField.Type type) {
		switch ( type ) {
			case DOUBLE: {
				return true;
			}
			case FLOAT: {
				return true;
			}
			case LONG: {
				return true;
			}
			case INT: {
				return true;
			}
			default: {
				return false;
			}
		}
	}

	public boolean isTextSortTypes(SortField.Type type) {
		switch ( type ) {
			case STRING: {
				return true;
			}
			case STRING_VAL: {
				return true;
			}
			default: {
				return false;
			}
		}
	}

	protected AbstractLuceneNumericFieldCodec<?, ?> getNumericCodec(SortField.Type type, Object indexNullAsValue) {
		switch ( type ) {
			case DOUBLE: {
				return (AbstractLuceneNumericFieldCodec<?, Double>) getCodec( Double.class, (Double) indexNullAsValue );
			}
			case FLOAT: {
				return (AbstractLuceneNumericFieldCodec<?, Float>) getCodec( Float.class, (Float) indexNullAsValue );
			}
			case LONG: {
				return (AbstractLuceneNumericFieldCodec<?, Long>) getCodec( Long.class, (Long) indexNullAsValue );
			}
			case INT: {
				return (AbstractLuceneNumericFieldCodec<?, Integer>) getCodec( Integer.class, (Integer) indexNullAsValue );
			}
			default: {
				throw new IllegalStateException( "Illegal sort type: " + type );
			}
		}
	}

	protected MultiValueMode getMultiValueMode(ScoreMode multi) {
		MultiValueMode sortMode = MultiValueMode.MIN;
		if ( multi != null ) {
			switch ( multi ) {
				case Min:
					sortMode = MultiValueMode.MIN;
					break;
				case Max:
					sortMode = MultiValueMode.MAX;
					break;
				case Avg:
					sortMode = MultiValueMode.AVG;
					break;
				case Total:
					sortMode = MultiValueMode.SUM;
					break;
			}
		}
		return sortMode;
	}

	protected Number getEffectiveMissingValue(AbstractLuceneNumericFieldCodec codec, Object missingValue, boolean reverse) {
		Number effectiveMissingValue;
		Number sortMissingValueFirstPlaceholder = codec.getDomain().getMinValue();
		Number sortMissingValueLastPlaceholder = codec.getDomain().getMaxValue();

		if ( missingValue == SortMissingValue.MISSING_FIRST ) {
			effectiveMissingValue = reverse ? sortMissingValueLastPlaceholder : sortMissingValueFirstPlaceholder;
		}
		else if ( missingValue == SortMissingValue.MISSING_LAST ) {
			effectiveMissingValue = reverse ? sortMissingValueFirstPlaceholder : sortMissingValueLastPlaceholder;
		}
		else {
			effectiveMissingValue = (Number) missingValue;
		}
		return effectiveMissingValue;
	}

}
