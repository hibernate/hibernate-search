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

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;

/**
 * Maps a property to an object field whose fields are the same as those defined in the property type.
 * <p>
 * This allows search queries on a single index to use data from multiple entities.
 * <p>
 * For example, let's consider this (incomplete) mapping:
 * <pre>{@code
 * {@literal @}Indexed
 * public class Hero {
 *     {@literal @}GenericField
 *     private String firstName;
 *     {@literal @}GenericField
 *     private String lastName;
 *     {@literal @}IndexedEmbedded
 *     private List<Sidekick> sidekicks;
 * }
 * public class Sidekick {
 *     {@literal @}GenericField
 *     private String firstName;
 *     {@literal @}GenericField
 *     private String lastName;
 *     private Hero hero;
 * }
 * }</pre>
 * <p>
 * The names of sidekicks are stored in different objects,
 * thus by default they would not be included in documents created
 * for {@code Hero} entities.
 * But we added the {@code @IndexedEmbedded} annotation to the {@code sidekicks} property,
 * so Hibernate Search will <em>embed</em> this data in a {@code sidekicks} field
 * of documents created for {@code Hero} entities.
 * <p>
 * How exactly this embedding will happen depends on the configured {@link #storage() storage type}.
 * Let's consider this representation of the hero Bruce Wayne:
 * <ul>
 *     <li>firstName = Bruce</li>
 *     <li>lastName = Wayne</li>
 *     <li>sidekicks =
 *         <ul>
 *             <li>(first element)
 *                 <ul>
 *                     <li>firstName = Dick</li>
 *                     <li>lastName = Grayson</li>
 *                 </ul>
 *             </li>
 *             <li>(second element)
 *                 <ul>
 *                     <li>firstName = Barbara</li>
 *                     <li>lastName = Gordon</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 * With the default {@link ObjectFieldStorage#FLATTENED flattened storage type} (more efficient),
 * the structure will be a little different from what one would expect:
 * <ul>
 *     <li>firstName = Bruce</li>
 *     <li>lastName = Wayne</li>
 *     <li>sidekicks.firstName =
 *         <ul>
 *             <li>(first element) Dick</li>
 *             <li>(second element) Barbara</li>
 *         </ul>
 *     </li>
 *     <li>sidekicks.lastName =
 *         <ul>
 *             <li>(first element) Grayson</li>
 *             <li>(second element) Gordon</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 * To get the original structure, the {@link ObjectFieldStorage#NESTED nested storage type} must be used,
 * but this has an impact on performance and how queries must be structured.
 * See the reference documentation for more information.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexedEmbedded {

	/**
	 * @return The prefix used when embedding. Defaults to {@code <property name>.},
	 * meaning an object field with the same name as the property
	 * will be defined in the parent document
	 * to host the value of the child document.
	 */
	String prefix() default "";

	/**
	 * The max recursion depth for indexed-embedded processing.
	 * <p>
	 * {@code maxDepth=0} means fields defined on the associated element are <strong>not</strong> indexed,
	 * nor is any field of embedded elements inside the associated element,
	 * unless they are included explicitly through {@link #includePaths()}.
	 * <p>
	 * {@code maxDepth=1} means fields defined on the associated element <strong>are</strong> indexed,
	 * but <strong>not</strong> fields of embedded elements inside the associated element,
	 * unless they are included explicitly through {@link #includePaths()}.
	 * <p>
	 * And so on. In short, the max depth is the number of {@code @IndexedEmbedded} that will be traversed
	 * and for which all fields will be included, even if they are not included explicitly through {@link #includePaths()}.
	 * <p>
	 * The default value depends on the value of the {@link #includePaths()} attribute:
	 * if {@link #includePaths()} is empty, the default is {@code Integer.MAX_VALUE} (no limit)
	 * if {@link #includePaths()} is <strong>not</strong> empty, the default is {@code 0}
	 * (only include fields included explicitly).
	 *
	 * @return The max depth size.
	 */
	int maxDepth() default -1;

	/**
	 * Defines paths of index fields from the associated object that should be embedded,
	 * even if they are beyond the {@link #maxDepth()}.
	 *
	 * @return The paths of index fields to include explicitly.
	 * Provided paths must be relative to the associated object,
	 * i.e. they must not include the {@link #prefix()}.
	 */
	String[] includePaths() default {};

	/**
	 * @return The storage strategy of the object field created for this indexed-embedded.
	 * @see ObjectFieldStorage
	 */
	ObjectFieldStorage storage() default ObjectFieldStorage.DEFAULT;

	/**
	 * @return A definition of container extractors to be applied to the property,
	 * allowing the definition of an indexed-embedded for container elements.
	 * This is useful when the property is of container type,
	 * for example a {@code Map<TypeA, TypeB>}:
	 * defining the extraction as {@code @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)}
	 * allows referencing map keys instead of map values.
	 * By default, Hibernate Search will try to apply a set of extractors for common container types.
	 * @see ContainerExtraction
	 */
	ContainerExtraction extraction() default @ContainerExtraction;

	// TODO HSEARCH-3071 includeEmbeddedObjectId
	// TODO HSEARCH-3072 targetElement

}
