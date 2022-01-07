/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.jar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

public final class JandexAnnotationUtils {
	private static final DotName RETENTION = DotName.createSimple( Retention.class.getName() );

	private JandexAnnotationUtils() {
	}

	public static Set<DotName> findRuntimeAnnotations(Index index) {
		Set<DotName> annotations = new HashSet<>();
		for ( AnnotationInstance retentionAnnotation : index.getAnnotations( RETENTION ) ) {
			ClassInfo annotation = retentionAnnotation.target().asClass();
			if ( RetentionPolicy.RUNTIME.name().equals( retentionAnnotation.value().asEnum() ) ) {
				annotations.add( annotation.name() );
			}
		}
		return annotations;
	}

	public static ClassInfo extractClass(AnnotationTarget target) {
		switch ( target.kind() ) {
			case CLASS:
				return target.asClass();
			case FIELD:
				return target.asField().declaringClass();
			case METHOD:
				return target.asMethod().declaringClass();
			case METHOD_PARAMETER:
				return target.asMethodParameter().method().declaringClass();
			case TYPE:
				return extractClass( target.asType().enclosingTarget() );
			default:
				throw new IllegalStateException( "Unsupported annotation target kind: " + target.kind() );
		}
	}
}
