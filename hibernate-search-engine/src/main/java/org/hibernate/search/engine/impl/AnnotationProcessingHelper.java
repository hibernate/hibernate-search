/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.engine.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * A helper classes dealing with the processing of annotation. It is there to share some annotation processing
 * between the document builder and other metadata classes, eg {@code FieldMetadata}. In the long run
 * this class might become obsolete.
 *
 * @author Hardy Ferentschik
 */
public final class AnnotationProcessingHelper {

	/**
	 * Using the passed field (or class bridge) settings determines the Lucene {@link org.apache.lucene.document.Field.Index}
	 *
	 * @param index is the field indexed or not
	 * @param analyze should the field be analyzed
	 * @param norms are norms to be added to index
	 *
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

	public static Float getBoost(XProperty member, org.hibernate.search.annotations.Field fieldAnn) {
		float computedBoost = 1.0f;
		Boost boostAnn = member.getAnnotation( Boost.class );
		if ( boostAnn != null ) {
			computedBoost = boostAnn.value();
		}
		if ( fieldAnn != null ) {
			computedBoost *= fieldAnn.boost().value();
		}
		return computedBoost;
	}

	public static BoostStrategy getDynamicBoost(XProperty member) {
		DynamicBoost boostAnnotation = member.getAnnotation( DynamicBoost.class );
		if ( boostAnnotation == null ) {
			return new DefaultBoostStrategy();
		}

		Class<? extends BoostStrategy> boostStrategyClass = boostAnnotation.impl();
		BoostStrategy strategy;
		try {
			strategy = boostStrategyClass.newInstance();
		}
		catch ( Exception e ) {
			throw new SearchException(
					"Unable to instantiate boost strategy implementation: " + boostStrategyClass.getName()
			);
		}
		return strategy;
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

	public static Analyzer getAnalyzer(org.hibernate.search.annotations.Analyzer analyzerAnn, ConfigContext context) {
		Class<?> analyzerClass = analyzerAnn == null ? void.class : analyzerAnn.impl();
		if ( analyzerClass == void.class ) {
			String definition = analyzerAnn == null ? "" : analyzerAnn.definition();
			if ( StringHelper.isEmpty( definition ) ) {
				return null;
			}
			else {
				return context.buildLazyAnalyzer( definition );
			}
		}
		else {
			try {
				return ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, context.getLuceneMatchVersion() );
			}
			catch ( ClassCastException e ) {
				throw new SearchException(
						"Lucene analyzer does not extend " + Analyzer.class.getName() + ": " + analyzerClass.getName(),
						e
				);
			}
			catch ( Exception e ) {
				throw new SearchException(
						"Failed to instantiate lucene analyzer with type " + analyzerClass.getName(), e
				);
			}
		}
	}

	public static Integer getPrecisionStep(NumericField numericFieldAnn) {
		return numericFieldAnn == null ? NumericField.PRECISION_STEP_DEFAULT : numericFieldAnn.precisionStep();
	}
}


