/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.impl;

import java.lang.annotation.Annotation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * A helper classes dealing with the processing of annotation. It is there to share some annotation processing
 * between the document builder and other metadata classes, eg {@code FieldMetadata}. In the long run
 * this class might become obsolete.
 *
 * @author Hardy Ferentschik
 */
public final class AnnotationProcessingHelper {

	private AnnotationProcessingHelper() {
		//not allowed
	}

	/**
	 * Using the passed field (or class bridge) settings determines the Lucene {@link org.apache.lucene.document.Field.Index}
	 *
	 * @param index is the field indexed or not
	 * @param analyze should the field be analyzed
	 * @param norms are norms to be added to index
	 * @return Returns the Lucene {@link org.apache.lucene.document.Field.Index} value for a given field
	 */
	public static Field.Index getIndex(Index index, Analyze analyze, Norms norms) {
		if ( Index.YES.equals( index ) ) {
			if ( Analyze.YES.equals( analyze ) ) {
				if ( Norms.YES.equals( norms ) ) {
					return Field.Index.ANALYZED;
				}
				else {
					return Field.Index.ANALYZED_NO_NORMS;
				}
			}
			else {
				if ( Norms.YES.equals( norms ) ) {
					return Field.Index.NOT_ANALYZED;
				}
				else {
					return Field.Index.NOT_ANALYZED_NO_NORMS;
				}
			}
		}
		else {
			return Field.Index.NO;
		}
	}

	public static Float getBoost(XProperty member, Annotation fieldAnn) {
		float computedBoost = 1.0f;
		Boost boostAnn = member.getAnnotation( Boost.class );
		if ( boostAnn != null ) {
			computedBoost = boostAnn.value();
		}
		if ( fieldAnn != null ) {
			float boost;
			if ( fieldAnn instanceof org.hibernate.search.annotations.Field ) {
				boost = ( (org.hibernate.search.annotations.Field) fieldAnn ).boost().value();
			}
			else if ( fieldAnn instanceof Spatial ) {
				boost = ( (Spatial) fieldAnn ).boost().value();
			}
			else {
				raiseAssertionOnIncorrectAnnotation( fieldAnn );
				boost = 0; //never reached
			}
			computedBoost *= boost;
		}
		return computedBoost;
	}

	public static BoostStrategy getDynamicBoost(final XAnnotatedElement element) {
		if ( element == null ) {
			return DefaultBoostStrategy.INSTANCE;
		}
		DynamicBoost boostAnnotation = element.getAnnotation( DynamicBoost.class );
		if ( boostAnnotation == null ) {
			return DefaultBoostStrategy.INSTANCE;
		}
		Class<? extends BoostStrategy> boostStrategyClass = boostAnnotation.impl();
		return ClassLoaderHelper.instanceFromClass( BoostStrategy.class, boostStrategyClass, "boost strategy" );
	}

	public static Field.TermVector getTermVector(TermVector vector) {
		switch ( vector ) {
			case NO:
				return Field.TermVector.NO;
			case YES:
				return Field.TermVector.YES;
			case WITH_OFFSETS:
				return Field.TermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return Field.TermVector.WITH_POSITIONS;
			case WITH_POSITION_OFFSETS:
				return Field.TermVector.WITH_POSITIONS_OFFSETS;
			default:
				throw new AssertionFailure( "Unexpected TermVector: " + vector );
		}
	}

	public static Analyzer getAnalyzer(org.hibernate.search.annotations.Analyzer analyzerAnn, ConfigContext configContext) {
		Class<?> analyzerClass = analyzerAnn == null ? void.class : analyzerAnn.impl();
		if ( analyzerClass == void.class ) {
			String definition = analyzerAnn == null ? "" : analyzerAnn.definition();
			if ( StringHelper.isEmpty( definition ) ) {
				return null;
			}
			else {
				return configContext.buildLazyAnalyzer( definition );
			}
		}
		else {
			try {
				return ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, configContext.getLuceneMatchVersion() );
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"Lucene analyzer does not extend " + Analyzer.class.getName() + ": " + analyzerClass.getName(),
						e
				);
			}
			catch (Exception e) {
				throw new SearchException(
						"Failed to instantiate lucene analyzer with type " + analyzerClass.getName(), e
				);
			}
		}
	}

	public static Integer getPrecisionStep(NumericField numericFieldAnn) {
		return numericFieldAnn == null ? NumericField.PRECISION_STEP_DEFAULT : numericFieldAnn.precisionStep();
	}

	public static String getFieldName(Annotation fieldAnn) {
		final String fieldName;
		if ( fieldAnn instanceof org.hibernate.search.annotations.Field ) {
			fieldName = ( (org.hibernate.search.annotations.Field) fieldAnn ).name();
		}
		else if ( fieldAnn instanceof Spatial ) {
			fieldName = ( (Spatial) fieldAnn ).name();
		}
		else if ( fieldAnn instanceof SortableField ) {
			fieldName = ( (SortableField) fieldAnn ).forField();
		}
		else if ( fieldAnn instanceof NumericField ) {
			fieldName = ( (NumericField) fieldAnn ).forField();
		}
		else {
			return raiseAssertionOnIncorrectAnnotation( fieldAnn );
		}
		return fieldName;
	}

	private static String raiseAssertionOnIncorrectAnnotation(Annotation fieldAnn) {
		throw new AssertionFailure( "Cannot process instances other than @Field, @Spatial and @SortableField. Found: " + fieldAnn.getClass() );
	}
}
