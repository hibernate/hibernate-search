/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.impl.BridgeFactory;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Encapsulating the metadata for a single {@code @Field} annotation.
 *
 * @author Hardy Ferentschik
 */
public class FieldMetadata {
	private static final Log log = LoggerFactory.make();

	private final XProperty fieldGetter;
	private final String fieldName;
	private final Store store;
	private final Field.Index index;
	private final Float boost;
	private final BoostStrategy dynamicBoostStrategy;
	private final Field.TermVector termVector;
	private final Integer precisionStep;
	private final String nullToken;
	private final FieldBridge fieldBridge;
	private final Analyzer analyzer;

	public FieldMetadata(String prefix,
						 XProperty member,
						 org.hibernate.search.annotations.Field fieldAnn,
						 NumericField numericFieldAnn,
						 ConfigContext context,
						 ReflectionManager reflectionManager) {
		ReflectionHelper.setAccessible( member );
		fieldGetter = member;
		fieldName = prefix + ReflectionHelper.getAttributeName( member, fieldAnn.name() );
		store = fieldAnn.store();
		index = AnnotationProcessingHelper.getIndex( fieldAnn.index(), fieldAnn.analyze(), fieldAnn.norms() );
		boost = AnnotationProcessingHelper.getBoost( member, fieldAnn );
		dynamicBoostStrategy = AnnotationProcessingHelper.getDynamicBoost( member );
		termVector = AnnotationProcessingHelper.getTermVector( fieldAnn.termVector() );
		precisionStep = AnnotationProcessingHelper.getPrecisionStep( numericFieldAnn );

		// null token
		String indexNullAs = fieldAnn.indexNullAs();
		if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DO_NOT_INDEX_NULL ) ) {
			indexNullAs = null;
		}
		else if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DEFAULT_NULL_TOKEN ) ) {
			indexNullAs = context.getDefaultNullToken();
		}
		nullToken = indexNullAs;

		FieldBridge bridge = BridgeFactory.guessType( fieldAnn, numericFieldAnn, member, reflectionManager );
		if ( indexNullAs != null && bridge instanceof TwoWayFieldBridge ) {
			bridge = new NullEncodingTwoWayFieldBridge( (TwoWayFieldBridge) bridge, indexNullAs );
		}
		fieldBridge = bridge;

		// Field > property > entity analyzer
		Analyzer tmpAnalyzer = AnnotationProcessingHelper.getAnalyzer( fieldAnn.analyzer(), context );
		if ( tmpAnalyzer == null ) {
			tmpAnalyzer = AnnotationProcessingHelper.getAnalyzer(
					member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
					context
			);
		}
		analyzer = tmpAnalyzer;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Field.Index getIndex() {
		return index;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void appendToPropertiesMetadata(AbstractDocumentBuilder.PropertiesMetadata propertiesMetadata) {
		sanityCheckFieldConfiguration( propertiesMetadata );

		propertiesMetadata.fieldGetters.add( fieldGetter );
		propertiesMetadata.fieldGetterNames.add( fieldGetter.getName() );
		propertiesMetadata.fieldNames.add( fieldName );
		propertiesMetadata.fieldNameToPositionMap.put( fieldGetter.getName(), propertiesMetadata.fieldNames.size() );
		propertiesMetadata.fieldStore.add( store );
		propertiesMetadata.fieldIndex.add( index );
		propertiesMetadata.fieldBoosts.add( boost );
		propertiesMetadata.dynamicFieldBoosts.add( dynamicBoostStrategy );
		propertiesMetadata.fieldTermVectors.add( termVector );
		propertiesMetadata.precisionSteps.add( precisionStep );
		propertiesMetadata.fieldNullTokens.add( nullToken );
		propertiesMetadata.fieldBridges.add( fieldBridge );
	}

	private void sanityCheckFieldConfiguration(AbstractDocumentBuilder.PropertiesMetadata propertiesMetadata) {
		int indexOfFieldWithSameName = propertiesMetadata.fieldNames.lastIndexOf( fieldName );
		if ( indexOfFieldWithSameName != -1 ) {
			if ( !propertiesMetadata.fieldIndex.get( indexOfFieldWithSameName ).equals( index ) ) {
				log.inconsistentFieldConfiguration( fieldName );
			}
		}
	}
}


