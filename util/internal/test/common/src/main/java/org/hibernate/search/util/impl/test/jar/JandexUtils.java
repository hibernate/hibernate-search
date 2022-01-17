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
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public final class JandexUtils {
	private static final DotName RETENTION = DotName.createSimple( Retention.class.getName() );

	private JandexUtils() {
	}

	public static Set<String> toStrings(Set<DotName> dotNames) {
		return dotNames.stream().map( DotName::toString ).collect( Collectors.toCollection( TreeSet::new ) );
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

	public static ClassInfo extractDeclaringClass(AnnotationTarget target) {
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
				return extractDeclaringClass( target.asType().enclosingTarget() );
			default:
				throw new IllegalStateException( "Unsupported annotation target kind: " + target.kind() );
		}
	}

	public static Set<DotName> collectClassHierarchiesRecursively(Index index, Set<DotName> initialClasses) {
		Set<DotName> classes = new HashSet<>();
		for ( DotName initialClass : initialClasses ) {
			collectClassHierarchiesRecursively( index, initialClass, classes );
		}
		return classes;
	}

	private static void collectClassHierarchiesRecursively(Index index, DotName className, Set<DotName> classes) {
		if ( className.toString().startsWith( "java." ) ) {
			return;
		}
		if ( classes.contains( className ) ) {
			return;
		}
		ClassInfo clazz = index.getClassByName( className );
		if ( clazz == null ) {
			return;
		}
		classes.add( className );
		collectClassHierarchiesRecursively( index, clazz.superName(), classes );
		for ( DotName interfaceName : clazz.interfaceNames() ) {
			collectClassHierarchiesRecursively( index, interfaceName, classes );
		}
		for ( ClassInfo subclass : index.getAllKnownSubclasses( className ) ) {
			collectClassHierarchiesRecursively( index, subclass.name(), classes );
		}
	}

	private static Set<DotName> collectModelClassesRecursively(Index index, Set<DotName> initialClasses) {
		Set<DotName> classes = new HashSet<>();
		for ( DotName initialClass : initialClasses ) {
			collectModelClassesRecursively( index, initialClass, classes );
		}
		return classes;
	}

	private static void collectModelClassesRecursively(Index index, DotName className, Set<DotName> classes) {
		if ( className.toString().startsWith( "java." ) ) {
			return;
		}
		if ( classes.contains( className ) ) {
			return;
		}
		ClassInfo clazz = index.getClassByName( className );
		if ( clazz == null ) {
			return;
		}
		classes.add( className );
		collectModelClassesRecursively( index, clazz.superName(), classes );
		for ( DotName interfaceName : clazz.interfaceNames() ) {
			collectModelClassesRecursively( index, interfaceName, classes );
		}
		for ( ClassInfo subclass : index.getAllKnownSubclasses( className ) ) {
			collectModelClassesRecursively( index, subclass.name(), classes );
		}

		for ( FieldInfo field : clazz.fields() ) {
			collectModelClassesRecursively( index, field.type(), classes );
		}
		for ( FieldInfo field : clazz.fields() ) {
			collectModelClassesRecursively( index, field.type(), classes );
		}
		for ( MethodInfo methodInfo : clazz.methods() ) {
			if ( !methodInfo.parameters().isEmpty() ) {
				// Definitely not a getter, just skip.
				continue;
			}
			collectModelClassesRecursively( index, methodInfo.returnType(), classes );
		}
	}

	private static void collectModelClassesRecursively(Index index, Type type, Set<DotName> classes) {
		switch ( type.kind() ) {
			case CLASS:
				collectModelClassesRecursively( index, type.name(), classes );
				break;
			case ARRAY:
				collectModelClassesRecursively( index, type.asArrayType().component(), classes );
				break;
			case TYPE_VARIABLE:
				for ( Type bound : type.asTypeVariable().bounds() ) {
					collectModelClassesRecursively( index, bound, classes );
				}
				break;
			case WILDCARD_TYPE:
				collectModelClassesRecursively( index, type.asWildcardType().extendsBound(), classes );
				collectModelClassesRecursively( index, type.asWildcardType().superBound(), classes );
				break;
			case PARAMETERIZED_TYPE:
				collectModelClassesRecursively( index, type.name(), classes );
				for ( Type argument : type.asParameterizedType().arguments() ) {
					collectModelClassesRecursively( index, argument, classes );
				}
				break;
			case PRIMITIVE:
			case VOID:
			case UNRESOLVED_TYPE_VARIABLE:
				// Ignore
				break;
		}
	}
}
