/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.utils;

import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;


public final class AnnotationUtils {

	private AnnotationUtils() {
	}

	public static boolean isIgnored(Element element) {
		return findAnnotation( element, HibernateSearchConfiguration.class )
				.flatMap( a -> a.attribute( "ignore", Boolean.class ) )
				.orElse( false );
	}

	public static Optional<AnnotationAttributeHolder> findAnnotation(Element element, Class<?> annotation) {
		for ( AnnotationMirror mirror : element.getAnnotationMirrors() ) {
			if ( mirror.getAnnotationType().toString().equals( annotation.getName() ) ) {
				return Optional.of( new AnnotationAttributeHolder( mirror ) );
			}
		}
		return Optional.empty();
	}

	public static class AnnotationAttributeHolder {
		private final AnnotationMirror annotationMirror;

		private AnnotationAttributeHolder(AnnotationMirror annotationMirror) {
			this.annotationMirror = annotationMirror;
		}

		public <T> Optional<T> attribute(String name, Class<T> klass) {
			return annotationMirror.getElementValues().entrySet().stream()
					.filter( entry -> entry.getKey().getSimpleName().contentEquals( name ) )
					.map( entry -> klass.cast( entry.getValue().getValue() ) )
					.findAny();
		}
	}

}
