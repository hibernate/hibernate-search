/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class MappingAnnotationProcessorUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private MappingAnnotationProcessorUtils() {
	}

	public static Optional<PojoModelPathValueNode> toPojoModelPathValueNode(ObjectPath objectPath) {
		PropertyValue[] inversePathElements = objectPath.value();
		PojoModelPath.Builder inversePathBuilder = PojoModelPath.builder();
		for ( PropertyValue element : inversePathElements ) {
			String inversePropertyName = element.propertyName();
			ContainerExtractorPath inverseExtractorPath = toContainerExtractorPath( element.extraction() );
			inversePathBuilder.property( inversePropertyName ).value( inverseExtractorPath );
		}
		return Optional.ofNullable( inversePathBuilder.toValuePathOrNull() );
	}

	public static ContainerExtractorPath toContainerExtractorPath(ContainerExtraction extraction) {
		ContainerExtract extract = extraction.extract();
		String[] extractors = extraction.value();
		switch ( extract ) {
			case NO:
				if ( extractors.length != 0 ) {
					throw log.cannotReferenceExtractorsWhenExtractionDisabled();
				}
				return ContainerExtractorPath.noExtractors();
			case DEFAULT:
				if ( extractors.length == 0 ) {
					return ContainerExtractorPath.defaultExtractors();
				}
				else {
					return ContainerExtractorPath.explicitExtractors( Arrays.asList( extractors ) );
				}
			default:
				throw new AssertionFailure(
						"Unexpected " + ContainerExtract.class.getSimpleName() + " value: " + extract
				);
		}
	}

	public static <T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name, BeanRetrieval retrieval) {
		String cleanedUpName = name.isEmpty() ? null : name;
		Class<? extends T> cleanedUpType = undefinedTypeMarker.equals( type ) ? null : type;
		if ( cleanedUpName == null && cleanedUpType == null ) {
			return Optional.empty();
		}
		else {
			Class<? extends T> defaultedType = cleanedUpType == null ? expectedType : cleanedUpType;
			return Optional.of( BeanReference.of( defaultedType, cleanedUpName, retrieval ) );
		}
	}

	public static Map<String, Object> toMap(Param[] params) {
		Contracts.assertNotNull( params, "params" );

		if ( params.length == 0 ) {
			return Collections.emptyMap();
		}

		Map<String, Object> map = new LinkedHashMap<>();
		for ( Param param : params ) {
			Object previous = map.put( param.name(), param.value() );
			if ( previous != null ) {
				throw log.conflictingParameterDefined( param.name(), param.value(), previous );
			}
		}
		return map;
	}

	public static Set<String> cleanUpPaths(String[] pathsArray) {
		Set<String> paths;
		if ( pathsArray.length > 0 ) {
			paths = new HashSet<>();
			Collections.addAll( paths, pathsArray );
		}
		else {
			paths = Collections.emptySet();
		}
		return paths;
	}
}
