/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.AssociationInverseSideProcessor;

/**
 * Given an association from an entity type {@code A} to an entity type {@code B},
 * defines the inverse side of an association,
 * i.e. the path from {@code B} to {@code A}.
 * <p>
 * This annotation is generally not needed, as inverse sides of associations should generally be inferred by the mapper.
 * For example, Hibernate ORM defines inverse sides using {@code @OneToMany#mappedBy}, {@code @OneToOne#mappedBy}, etc.,
 * and the Hibernate ORM mapper will register these inverse sides automatically.
 * <p>
 * This annotation may be applied multiple times to the same property with different {@link #extraction() extractions},
 * to configure a different association for different container elements.
 * For example with a property of type {@code Map<Entity1, Entity2>},
 * one can have an association to {@code Entity1} (map keys) or {@code Entity2} (map values).
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AssociationInverseSide.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = AssociationInverseSideProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface AssociationInverseSide {

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the definition of the inverse side of an association modeled by container elements.
	 * This is useful when the property is of container type,
	 * for example a {@code Map<EntityA, EntityB>}:
	 * defining the extraction as {@code @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)}
	 * allows referring to the association modeled by the map keys instead of the one modeled by the map values.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see ContainerExtraction
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	/**
	 * @return The path to the targeted entity on the inverse side of the association.
	 */
	ObjectPath inversePath();

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		AssociationInverseSide[] value();
	}

}
