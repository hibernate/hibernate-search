/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.util.common.jar.impl.JandexUtils;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public final class AnnotationScanning {

	private AnnotationScanning() {
	}

	public static Set<DotName> scanForRootMappingAnnotatedTypes(IndexView jandexIndex) {
		Set<DotName> rootMappingAnnotations = new HashSet<>( BuiltinAnnotations.ROOT_MAPPING_ANNOTATIONS );
		rootMappingAnnotations.addAll(
				JandexUtils.findAnnotatedAnnotationsAndContaining( jandexIndex, BuiltinAnnotations.ROOT_MAPPING ) );

		Set<DotName> rootMappingAnnotatedTypes = new HashSet<>();
		for ( DotName annotationName : rootMappingAnnotations ) {
			for ( AnnotationInstance annotation : jandexIndex.getAnnotations( annotationName ) ) {
				ClassInfo annotatedClassInfo = JandexUtils.extractDeclaringClass( annotation.target() );
				rootMappingAnnotatedTypes.add( annotatedClassInfo.name() );
			}
		}
		return rootMappingAnnotatedTypes;
	}

}
