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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.IndexedEmbeddedProcessor;

/**
 * Maps a property to an object field whose fields are the same as those defined in the property type.
 * <p>
 * This allows search queries on a single index to use data from multiple entities.
 * <p>
 * For example, let's consider this (incomplete) mapping:
 * <pre>{@code
 * {@literal @}Indexed
 * public class Book {
 *     {@literal @}GenericField
 *     private String title;
 *     {@literal @}IndexedEmbedded
 *     private List<Author> authors;
 * }
 * public class Author {
 *     {@literal @}GenericField
 *     private String firstName;
 *     {@literal @}GenericField
 *     private String lastName;
 *     private List<Book> books;
 * }
 * }</pre>
 * <p>
 * The names of authors are stored in different objects,
 * thus by default they would not be included in documents created
 * for {@code Book} entities.
 * But we added the {@code @IndexedEmbedded} annotation to the {@code authors} property,
 * so Hibernate Search will <em>embed</em> this data in a {@code authors} field
 * of documents created for {@code Book} entities.
 * <p>
 * How exactly this embedding will happen depends on the configured {@link #storage() storage type}.
 * Let's consider this representation of the book "Levianthan Wakes":
 * <ul>
 *     <li>title = Levianthan Wakes</li>
 *     <li>authors =
 *         <ul>
 *             <li>(first element)
 *                 <ul>
 *                     <li>firstName = Daniel</li>
 *                     <li>lastName = Abraham</li>
 *                 </ul>
 *             </li>
 *             <li>(second element)
 *                 <ul>
 *                     <li>firstName = Ty</li>
 *                     <li>lastName = Frank</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 * With the default {@link ObjectFieldStorage#FLATTENED flattened storage type} (more efficient),
 * the structure will be a little different from what one would expect:
 * <ul>
 *     <li>title = Levianthan Wakes</li>
 *     <li>authors.firstName =
 *         <ul>
 *             <li>(first element) Daniel</li>
 *             <li>(second element) Ty</li>
 *         </ul>
 *     </li>
 *     <li>authors.lastName =
 *         <ul>
 *             <li>(first element) Abraham</li>
 *             <li>(second element) Frank</li>
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
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = IndexedEmbeddedProcessor.class))
public @interface IndexedEmbedded {

	/**
	 * @return The prefix used when embedding. Defaults to {@code <property name>.},
	 * meaning an object field with the same name as the property
	 * will be defined in the parent document
	 * to host the value of the child document.
	 */
	String prefix() default "";

	/**
	 * The paths of index fields from the indexed-embedded element that should be embedded.
	 * <p>
	 * This takes precedence over {@link #maxDepth()}.
	 * <p>
	 * By default, if neither {@code includePaths} nor {@link #maxDepth()} is defined,
	 * all index fields are included.
	 *
	 * @return The paths of index fields to include explicitly.
	 * Provided paths must be relative to the indexed-embedded element,
	 * i.e. they must not include the {@link #prefix()}.
	 */
	String[] includePaths() default {};

	/**
	 * The max recursion depth for indexed-embedded processing.
	 * <p>
	 * {@code maxDepth} is the number of `@IndexedEmbedded` that will be traversed
	 * and for which all fields of the indexed-embedded element will be included,
	 * even if these fields are not included explicitly through {@code includePaths}:
	 * <ul>
	 * <li>{@code maxDepth=0} means fields of the indexed-embedded element are <strong>not</strong> included,
	 * nor is any field of nested indexed-embedded elements,
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>{@code maxDepth=1} means fields of the indexed-embedded element <strong>are</strong> included,
	 * but <strong>not</strong> fields of nested indexed-embedded elements,
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>And so on.
	 * </ul>
	 * The default value depends on the value of the {@link #includePaths()} attribute:
	 * if {@link #includePaths()} is empty, the default is {@code Integer.MAX_VALUE} (no limit)
	 * if {@link #includePaths()} is <strong>not</strong> empty, the default is {@code 0}
	 * (only include fields included explicitly).
	 *
	 * @return The max depth size.
	 */
	int maxDepth() default -1;

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
