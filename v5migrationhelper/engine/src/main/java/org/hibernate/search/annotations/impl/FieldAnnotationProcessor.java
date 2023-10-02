/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.impl.TruncatingCalendarBridge;
import org.hibernate.search.bridge.builtin.impl.TruncatingDateBridge;
import org.hibernate.search.bridge.builtin.impl.Truncation;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedProperty;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingGenericFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

@Deprecated
public class FieldAnnotationProcessor implements PropertyMappingAnnotationProcessor<Field> {
	private static final String LEGACY_DEFAULT_NULL_TOKEN = "__DEFAULT_NULL_TOKEN__";

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void process(PropertyMappingStep mapping, Field annotation,
			PropertyMappingAnnotationProcessorContext context) {
		// the following is an approximation. we're trying here to emulate the HS5's annotation behavior.
		// but some concepts of HS5, such as `DEFAULT_NULL_TOKEN`, have no equivalent in Search 6.

		MappingAnnotatedProperty annotatedProperty = context.annotatedElement();
		Class<?> propertyType = annotatedProperty.javaClass( ContainerExtractorPath.defaultExtractors() )
				.get(); // The default extractors can always be applied: worst case, they default to no extractors.

		PropertyMappingStandardFieldOptionsStep<?> optionsStep;
		if ( String.class.equals( propertyType )
				|| Enum.class.isAssignableFrom( propertyType )
				|| Character.class.equals( propertyType ) ) {
			if ( Analyze.YES.equals( annotation.analyze() ) ) {
				optionsStep = mapAnalyzed( mapping, annotation, context );
			}
			else {
				optionsStep = mapKeyword( mapping, annotation, context, null );
			}
		}
		else {
			optionsStep = mapGeneric( mapping, annotation, context );
		}

		optionsStep
				.searchable( Index.YES.equals( annotation.index() ) ? Searchable.YES : Searchable.NO )
				.projectable( Store.YES.equals( annotation.store() ) ? Projectable.YES : Projectable.NO );
	}

	private PropertyMappingGenericFieldOptionsStep mapGeneric(PropertyMappingStep mapping, Field annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String name = name( annotation, context );

		PropertyMappingGenericFieldOptionsStep genericOptionsStep = mapping.genericField( name )
				.sortable( sortable( name, context ) ? Sortable.YES : Sortable.NO );

		String indexNullAs = indexNullAs( annotation, context );
		if ( indexNullAs != null ) {
			genericOptionsStep = genericOptionsStep.indexNullAs( indexNullAs );
		}

		BeanReference<? extends ValueBridge<?, ?>> valueBridge = valueBridge( context );
		if ( valueBridge != null ) {
			genericOptionsStep = genericOptionsStep.valueBridge( valueBridge );
		}

		List<String> facetNames = facetNames( name, context );
		for ( String facetName : facetNames ) {
			if ( facetName.isEmpty() || facetName.equals( name ) ) {
				genericOptionsStep = genericOptionsStep.aggregable( Aggregable.YES );
			}
			else {
				PropertyMappingGenericFieldOptionsStep facetOptionsStep = mapping.genericField( facetName )
						.aggregable( Aggregable.YES )
						.searchable( Searchable.NO );
				if ( valueBridge != null ) {
					facetOptionsStep = facetOptionsStep.valueBridge( valueBridge );
				}
				if ( indexNullAs != null ) {
					facetOptionsStep = facetOptionsStep.indexNullAs( indexNullAs );
				}
			}
		}

		return genericOptionsStep;
	}

	private PropertyMappingStandardFieldOptionsStep<?> mapAnalyzed(PropertyMappingStep mapping, Field annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String name = name( annotation, context );

		String analyzer = annotation.analyzer().definition();
		if ( analyzer.isEmpty() ) {
			analyzer = null;
		}
		String normalizer = annotation.normalizer().definition();
		if ( normalizer.isEmpty() ) {
			normalizer = null;
		}

		if ( analyzer != null && normalizer != null ) {
			/*
			 * @Field(normalizer = @Normalizer(definition = "..."), analyzer = @Analyzer(definition = "..."))
			 * private String myProperty;
			 */
			throw log.cannotReferenceAnalyzerAndNormalizer( name );
		}

		if ( normalizer != null ) {
			return mapKeyword( mapping, annotation, context, normalizer );
		}
		else {
			String analyzerOrDefault = analyzerOrDefault( context, analyzer );
			return mapFullText( mapping, annotation, context, analyzerOrDefault );
		}
	}

	private PropertyMappingStandardFieldOptionsStep<?> mapFullText(PropertyMappingStep mapping, Field annotation,
			PropertyMappingAnnotationProcessorContext context, String analyzerOrDefault) {
		String name = name( annotation, context );

		PropertyMappingFullTextFieldOptionsStep fullTextOptionsStep = mapping.fullTextField( name )
				.analyzer( analyzerOrDefault )
				.norms( norms( annotation ) )
				.termVector( termVector( annotation ) );

		if ( sortable( name, context ) ) {
			throw log.cannotUseAnalyzerOnSortableField( analyzerOrDefault );
		}

		if ( !facetNames( name, context ).isEmpty() ) {
			throw log.cannotUseAnalyzerOnFacetField( analyzerOrDefault );
		}

		String indexNullAs = indexNullAs( annotation, context );
		if ( indexNullAs != null ) {
			throw log.cannotUseIndexNullAsAndAnalyzer( analyzerOrDefault, indexNullAs );
		}

		return fullTextOptionsStep;
	}

	private PropertyMappingKeywordFieldOptionsStep mapKeyword(PropertyMappingStep mapping, Field annotation,
			PropertyMappingAnnotationProcessorContext context, String normalizer) {
		String name = name( annotation, context );

		PropertyMappingKeywordFieldOptionsStep keywordOptionsStep = mapping.keywordField( name )
				.norms( norms( annotation ) )
				.sortable( sortable( name, context ) ? Sortable.YES : Sortable.NO );

		if ( normalizer != null ) {
			keywordOptionsStep = keywordOptionsStep.normalizer( normalizer );
		}

		String indexNullAs = indexNullAs( annotation, context );
		if ( indexNullAs != null ) {
			keywordOptionsStep = keywordOptionsStep.indexNullAs( indexNullAs );
		}

		List<String> facetNames = facetNames( name, context );
		for ( String facetName : facetNames ) {
			if ( facetName.isEmpty() || facetName.equals( name ) ) {
				keywordOptionsStep = keywordOptionsStep.aggregable( Aggregable.YES );
			}
			else {
				PropertyMappingKeywordFieldOptionsStep facetOptionsStep = mapping.keywordField( facetName )
						.aggregable( Aggregable.YES )
						.searchable( Searchable.NO );
				if ( normalizer != null ) {
					facetOptionsStep = facetOptionsStep.normalizer( normalizer );
				}
				if ( indexNullAs != null ) {
					facetOptionsStep = facetOptionsStep.indexNullAs( indexNullAs );
				}
			}
		}

		return keywordOptionsStep;
	}

	private String name(Field annotation, PropertyMappingAnnotationProcessorContext context) {
		MappingAnnotatedProperty annotatedProperty = context.annotatedElement();
		return annotation.name().isEmpty() ? annotatedProperty.name() : annotation.name();
	}

	private String analyzerOrDefault(PropertyMappingAnnotationProcessorContext context,
			String fieldAnnotationAnalyzer) {
		if ( fieldAnnotationAnalyzer != null ) {
			return fieldAnnotationAnalyzer;
		}

		MappingAnnotatedProperty annotatedProperty = context.annotatedElement();
		Optional<String> propertyLevelAnalyzer = annotatedProperty.allAnnotations()
				.filter( a -> Analyzer.class.equals( a.annotationType() ) )
				.map( a -> ( (Analyzer) a ).definition() )
				.filter( a -> !a.isEmpty() )
				.findAny();
		return propertyLevelAnalyzer.orElse( AnalyzerNames.DEFAULT );
	}

	private String indexNullAs(Field annotation, PropertyMappingAnnotationProcessorContext context) {
		String indexNullAs = annotation.indexNullAs();
		if ( LEGACY_DEFAULT_NULL_TOKEN.equals( indexNullAs ) ) {
			throw log.defaultNullTokenNotSupported( context.annotatedElement().javaClass() );
		}
		if ( Field.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			return null;
		}
		return indexNullAs;
	}

	private boolean sortable(String fieldName, PropertyMappingAnnotationProcessorContext context) {
		MappingAnnotatedProperty annotatedProperty = context.annotatedElement();
		return annotatedProperty.allAnnotations()
				.filter( a -> SortableField.class.equals( a.annotationType() ) )
				.map( a -> ( (SortableField) a ).forField() )
				.anyMatch( forField -> forField.equals( fieldName )
						|| forField.isEmpty() && fieldName.equals( annotatedProperty.name() ) );
	}

	private List<String> facetNames(String fieldName, PropertyMappingAnnotationProcessorContext context) {
		MappingAnnotatedProperty annotatedProperty = context.annotatedElement();
		return annotatedProperty.allAnnotations()
				.filter( a -> Facet.class.equals( a.annotationType() ) )
				.map( a -> (Facet) a )
				.filter( a -> a.forField().equals( fieldName )
						|| a.forField().isEmpty() && fieldName.equals( annotatedProperty.name() ) )
				.map( Facet::name )
				.collect( Collectors.toList() );
	}

	// Converts the Search 5 Norms type to the Search 6 Norms type
	private Norms norms(Field annotation) {
		org.hibernate.search.annotations.Norms value = annotation.norms();
		switch ( value ) {
			case YES:
				return Norms.YES;
			case NO:
				return Norms.NO;
			default:
				throw new AssertionFailure( "Unknown value: " + value );
		}
	}

	// Converts the Search 5 TermVector type to the Search 6 TermVector type
	private TermVector termVector(Field annotation) {
		org.hibernate.search.annotations.TermVector value = annotation.termVector();
		switch ( value ) {
			case YES:
				return TermVector.YES; // This is the Search 6 TermVector, a different type.
			case NO:
				return TermVector.NO;
			case WITH_OFFSETS:
				return TermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return TermVector.WITH_POSITIONS;
			case WITH_POSITION_OFFSETS:
				return TermVector.WITH_POSITIONS_OFFSETS;
			default:
				throw new AssertionFailure( "Unknown value: " + annotation.termVector() );
		}
	}

	private BeanReference<? extends ValueBridge<?, ?>> valueBridge(PropertyMappingAnnotationProcessorContext context) {
		MappingAnnotatedProperty annotatedProperty = context.annotatedElement();
		return annotatedProperty.allAnnotations()
				.map( a -> valueBridge( annotatedProperty, a ) )
				.filter( Objects::nonNull )
				.findAny()
				.orElse( null );
	}

	private BeanReference<? extends ValueBridge<?, ?>> valueBridge(MappingAnnotatedProperty annotatedProperty,
			Annotation annotation) {
		if ( Date.class.isAssignableFrom( annotatedProperty.javaClass() )
				&& annotation.annotationType().equals( DateBridge.class ) ) {
			DateBridge castedAnnotation = (DateBridge) annotation;
			Truncation truncation = Truncation.at( castedAnnotation.resolution() );
			return BeanReference.ofInstance( new TruncatingDateBridge( truncation ) );
		}
		else if ( Calendar.class.isAssignableFrom( annotatedProperty.javaClass() )
				&& annotation.annotationType().equals( CalendarBridge.class ) ) {
			CalendarBridge castedAnnotation = (CalendarBridge) annotation;
			Truncation truncation = Truncation.at( castedAnnotation.resolution() );
			return BeanReference.ofInstance( new TruncatingCalendarBridge( truncation ) );
		}
		else {
			return null;
		}
	}
}
