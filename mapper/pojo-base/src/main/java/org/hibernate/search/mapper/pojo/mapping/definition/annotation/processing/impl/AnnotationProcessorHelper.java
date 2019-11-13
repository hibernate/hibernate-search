/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class AnnotationProcessorHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public AnnotationProcessorHelper() {
	}

	Optional<PojoModelPathValueNode> getPojoModelPathValueNode(ObjectPath objectPath) {
		PropertyValue[] inversePathElements = objectPath.value();
		PojoModelPath.Builder inversePathBuilder = PojoModelPath.builder();
		for ( PropertyValue element : inversePathElements ) {
			String inversePropertyName = element.propertyName();
			ContainerExtractorPath inverseExtractorPath = getExtractorPath( element.extraction() );
			inversePathBuilder.property( inversePropertyName ).value( inverseExtractorPath );
		}
		return Optional.ofNullable( inversePathBuilder.toValuePathOrNull() );
	}

	ContainerExtractorPath getExtractorPath(ContainerExtraction extraction) {
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

	<T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name) {
		String cleanedUpName = name.isEmpty() ? null : name;
		Class<? extends T> cleanedUpType = undefinedTypeMarker.equals( type ) ? null : type;
		if ( cleanedUpName == null && cleanedUpType == null ) {
			return Optional.empty();
		}
		else {
			Class<? extends T> defaultedType = cleanedUpType == null ? expectedType : cleanedUpType;
			return Optional.of( BeanReference.of( defaultedType, cleanedUpName ) );
		}
	}

}
