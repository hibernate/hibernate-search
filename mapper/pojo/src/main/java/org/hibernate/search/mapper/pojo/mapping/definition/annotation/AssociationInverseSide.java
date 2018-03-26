/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;

/**
 * Given an association on a entity type A to entity type B, defines the inverse side of an association,
 * i.e. the path from B to A.
 * <p>
 * This annotation is generally not needed, as inverse sides of associations should generally be inferred by the mapper.
 * For example, Hibernate ORM defines inverse sides using {@code @OneToMany#mappedBy}, {@code @OneToOne#mappedBy}, etc.,
 * and the Hibernate ORM mapper will register these inverse sides automatically.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AssociationInverseSide {

	/**
	 * @return An array of reference to container value extractor implementation classes,
	 * allowing to precisely define which association the inverse side is being defined for.
	 * For instance, on a property of type {@code Map<EntityA, EntityB>},
	 * {@code @AssociationInverseSide(extractors = @ContainerValueExtractorBeanReference(type = MapKeyExtractor.class))}
	 * would define the inverse side of the association for the map keys (of type EntityA),
	 * while {@code @AssociationInverseSide(extractors = @ContainerValueExtractorBeanReference(type = MapValueExtractor.class))}
	 * would define the inverse side of the association for the map values (of type EntityB).
	 * By default, Hibernate Search will try to apply a set of extractors for common types
	 * ({@link Iterable}, {@link java.util.Collection}, {@link java.util.Optional}, ...).
	 * To prevent Hibernate Search from applying any extractor, set this attribute to an empty array (<code>{}</code>).
	 */
	ContainerValueExtractorBeanReference[] extractors()
			default @ContainerValueExtractorBeanReference( type = DefaultExtractors.class );

	/**
	 * @return The name of the inverse property on the inverse side of the association
	 */
	String inverseProperty();

	/**
	 * @return An array of reference to container value extractor implementation classes,
	 * which will be applied to the inverse property when getting the value of the inverse side of the association.
	 * By default, Hibernate Search will try to apply a set of extractors for common types
	 * ({@link Iterable}, {@link java.util.Collection}, {@link java.util.Optional}, ...).
	 * To prevent Hibernate Search from applying any extractor, set this attribute to an empty array (<code>{}</code>).
	 */
	ContainerValueExtractorBeanReference[] inverseExtractors()
			default @ContainerValueExtractorBeanReference( type = DefaultExtractors.class );

	/**
	 * Class used as a marker for the default value of the {@link #extractors()} and {@link #inverseExtractors()}
	 * attributes.
	 */
	abstract class DefaultExtractors implements ContainerValueExtractor<Object, Object> {
		private DefaultExtractors() {
		}
	}

}
