/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

/**
 * Reference a type bridge for a {@link TypeBinding}.
 * <p>
 * References can use either a name, a type, or both.
 * <p>
 * Each time the mapped annotation is encountered, an instance of the type binder will be created.
 * The binder will be passed the annotation through its {@link TypeBinder#initialize(Annotation)} method,
 * and then the bridge will be created and bound by {@link TypeBinder#bind(TypeBindingContext)}.
 * <p>
 * Type bridges mapped this way can be parameterized:
 * the binder will be able to take any attribute of the mapped annotation into account
 * in its {@link TypeBinder#initialize(Annotation)} method.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeBridgeRef {

	/**
	 * Reference a type bridge by the bean name of its binder.
	 * @return The bean name of the type binder.
	 */
	String binderName() default "";

	/**
	 * Reference a type bridge by the type of its binder.
	 * @return The type of the type binder.
	 */
	Class<? extends TypeBinder<?>> binderType() default UndefinedBinderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #binderType()} attribute.
	 */
	abstract class UndefinedBinderImplementationType implements TypeBinder<Annotation> {
		private UndefinedBinderImplementationType() {
		}
	}
}

