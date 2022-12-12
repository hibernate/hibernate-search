/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.configuration.properties.collector.utils;

import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;


public final class AnnotationUtils {

	private AnnotationUtils() {
	}

	public static Optional<HibernateSearchConfiguration.Type> findType(Element element) {
		return element.getAnnotationMirrors().stream()
				.filter( mirror -> mirror.getAnnotationType().toString()
						.equals( HibernateSearchConfiguration.class.getName() ) )
				.map( mirror -> getAnnotationValue( mirror, "type" ) )
				.filter( Objects::nonNull )
				.filter( Optional::isPresent )
				.map( opt -> ( (HibernateSearchConfiguration.Type) opt.get() ) )
				.findAny();
	}

	public static boolean isIgnored(Element element) {
		return element.getAnnotationMirrors().stream()
				.filter( mirror -> mirror.getAnnotationType().toString()
						.equals( HibernateSearchConfiguration.class.getName() ) )
				.map( mirror -> getAnnotationValue( mirror, "ignore" ).orElse( null ) )
				.filter( Objects::nonNull )
				.anyMatch( Boolean.TRUE::equals );
	}

	private static Optional<Object> getAnnotationValue(AnnotationMirror annotationMirror, String name) {
		return annotationMirror.getElementValues().entrySet().stream()
				.filter( entry -> entry.getKey().getSimpleName().contentEquals( name ) )
				.map( entry -> entry.getValue().getValue() )
				.findAny();
	}

}
