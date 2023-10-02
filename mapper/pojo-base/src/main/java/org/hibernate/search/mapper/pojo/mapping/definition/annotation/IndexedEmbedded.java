/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.IndexedEmbeddedProcessor;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.annotation.Search5DeprecatedAPI;

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
 * How exactly this embedding will happen depends on the configured {@link #structure() structure}.
 * Let's consider this representation of the book "Leviathan Wakes":
 * <ul>
 *     <li>title = Leviathan Wakes</li>
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
 * With the default {@link ObjectStructure#FLATTENED flattened structure} (more efficient),
 * the document structure will be a little different from what one would expect:
 * <ul>
 *     <li>title = Leviathan Wakes</li>
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
 * To get the original structure, the {@link ObjectStructure#NESTED nested structure} must be used,
 * but this has an impact on performance and how queries must be structured.
 * See the reference documentation for more information.
 */
@Documented
@Repeatable(IndexedEmbedded.List.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = IndexedEmbeddedProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface IndexedEmbedded {

	/**
	 * @return The prefix used when embedding. Defaults to {@code <property name>.},
	 * meaning an object field with the same name as the property
	 * will be defined in the parent document
	 * to host the value of the child document.
	 * Must not be set if {@link #name()} is set.
	 * @deprecated Use {@link #name()} instead. Note that {@link #name()} does not allow dots.
	 */
	@Deprecated
	@Search5DeprecatedAPI
	String prefix() default "";

	/**
	 * @return The name of the object field created to represent this {@code @IndexedEmbedded}.
	 * Defaults to the property name.
	 * Must not be set if {@link #prefix()} is set.
	 */
	String name() default "";

	/**
	 * The paths of index fields from the indexed-embedded element that should be embedded.
	 * <p>
	 * This takes precedence over {@link #includeDepth()}.
	 * <p>
	 * By default, if none of {@code includePaths}, {@code excludePaths} or {@link #includeDepth()} are defined,
	 * all index fields are included.
	 * <p>
	 * Cannot be used when {@link #excludePaths()} contains any paths.
	 *
	 * @return The paths of index fields to include explicitly.
	 * Provided paths must be relative to the indexed-embedded element,
	 * i.e. they must not include the {@link #name()}.
	 * Paths <b>must</b> lead to a field and <b>cannot</b> end with some {@link #prefix() prefix used to construct a field name}.
	 */
	String[] includePaths() default { };


	/**
	 * The paths of index fields from the indexed-embedded element that should not be embedded.
	 * <p>
	 * This takes precedence over {@link #includeDepth()}.
	 * <p>
	 * By default, if none of {@code includePaths}, {@code excludePaths} or {@link #includeDepth()} are defined,
	 * all index fields are included.
	 * <p>
	 * Cannot be used when {@link #includePaths()} contains any paths.
	 *
	 * @return The paths of index fields to exclude explicitly.
	 * Provided paths must be relative to the indexed-embedded element,
	 * i.e. they must not include the {@link #name()}.
	 * Paths <b>must</b> lead to a field and <b>cannot</b> end with some {@link #prefix() prefix used to construct a field name}.
	 */
	@Incubating
	String[] excludePaths() default { };

	/**
	 * The number of levels of indexed-embedded that will have all their fields included by default.
	 * <p>
	 * {@code includeDepth} is the number of `@IndexedEmbedded` that will be traversed
	 * and for which all fields of the indexed-embedded element will be included,
	 * even if these fields are not included explicitly through {@code includePaths},
	 * unless these fields are excluded explicitly through {@code excludePaths}:
	 * <ul>
	 * <li>{@code includeDepth=0} means fields of this indexed-embedded element are <strong>not</strong> included,
	 * nor is any field of nested indexed-embedded elements,
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>{@code includeDepth=1} means fields of this indexed-embedded element <strong>are</strong> included,
	 * unless these fields are explicitly excluded through {@code excludePaths},
	 * but <strong>not</strong> fields of nested indexed-embedded elements ({@code @IndexedEmbedded} within this {@code @IndexedEmbedded}),
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>{@code includeDepth=2} means fields of this indexed-embedded element <strong>are</strong> included,
	 * and so are fields of the immediately nested indexed-embedded elements ({@code @IndexedEmbedded} within this {@code @IndexedEmbedded}),
	 * unless these fields are explicitly excluded through {@code excludePaths},
	 * but <strong>not</strong> fields of nested indexed-embedded elements beyond that
	 * ({@code @IndexedEmbedded} within an {@code @IndexedEmbedded} within this {@code @IndexedEmbedded}),
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>And so on.
	 * </ul>
	 * The default value depends on the value of {@link #includePaths()} attributes:
	 * <ul>
	 * <li>if {@link #includePaths()} is empty, the default is {@code Integer.MAX_VALUE} (include all fields at every level)</li>
	 * <li>if {@link #includePaths()} is <strong>not</strong> empty, the default is {@code 0} (only include fields included explicitly).</li>
	 * </ul>
	 *
	 * @return The number of levels of indexed-embedded that will have all their fields included by default.
	 */
	int includeDepth() default -1;

	/**
	 * Whether the identifier of embedded objects should be included as an index field.
	 * <p>
	 * The index field will be defined as if the following annotation was put on the identifier property
	 * of the embedded type:
	 * {@code @GenericField(searchable = Searchable.YES, projectable = Projectable.YES)}.
	 * The name of the index field will be the name of the identifier property.
	 * Its bridge will be the identifier bridge referenced by the embedded type's {@link DocumentId} annotation, if any,
	 * or the default value bridge for the identifier property's type, by default.
	 * <p>
	 * If you need more advanced mapping (custom name, custom bridge, sortable, ...),
	 * define the field explicitly in the embedded type by annotating the identifier property
	 * with {@link GenericField} or a similar field annotation,
	 * and make sure the field is included by {@code @IndexedEmbedded} by configuring
	 * {@link #includeDepth()} and/or {@link #includePaths()}.
	 *
	 * @return Whether the identifier of embedded objects should be included as an index field.
	 */
	boolean includeEmbeddedObjectId() default false;

	/**
	 * @return How the structure of the object field created for this indexed-embedded
	 * is preserved upon indexing.
	 * @see ObjectStructure
	 */
	ObjectStructure structure() default ObjectStructure.DEFAULT;

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

	/**
	 * @return A type indexed-embedded elements should be cast to.
	 * When relying on {@link #extraction() container extraction},
	 * the extracted values are cast, not the container.
	 * By default, no casting occurs.
	 */
	Class<?> targetType() default void.class;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		IndexedEmbedded[] value();
	}

}
