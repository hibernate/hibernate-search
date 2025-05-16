/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;


import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.sort.DistanceSortFieldReference;
import org.hibernate.search.engine.search.reference.sort.FieldSortFieldReference;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A factory for search sorts.
 *
 * <h2 id="field-paths">Field paths</h2>
 *
 * By default, field paths passed to this DSL are interpreted as absolute,
 * i.e. relative to the index root.
 * <p>
 * However, a new, "relative" factory can be created with {@link #withRoot(String)}:
 * the new factory interprets paths as relative to the object field passed as argument to the method.
 * <p>
 * This can be useful when calling reusable methods that can apply the same sort
 * on different object fields that have same structure (same sub-fields).
 * <p>
 * Such a factory can also transform relative paths into absolute paths using {@link #toAbsolutePath(String)};
 * this can be useful for native sorts in particular.
 *
 *
 * <h2 id="field-references">Field references</h2>
 *
 * A {@link org.hibernate.search.engine.search.reference field reference} is always represented by the absolute field path and,
 * if applicable, i.e. when a field reference is typed, a combination of the {@link org.hibernate.search.engine.search.common.ValueModel} and the type.
 * <p>
 * Field references are usually accessed from the generated Hibernate Search's static metamodel classes that describe the index structure.
 * Such reference provides the information on which search capabilities the particular index field possesses, and allows switching between different
 * {@link org.hibernate.search.engine.search.common.ValueModel value model representations}.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SearchSortFactory extends TypedSearchSortFactory<NonStaticMetamodelScope> {

	/**
	 * Order elements by the value of a specific field.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field to sort by.
	 * @return A DSL step where the "field" sort can be defined in more details.
	 * @throws SearchException If the field doesn't exist or cannot be sorted on.
	 */
	FieldSortOptionsStep<NonStaticMetamodelScope, ?, ? extends SearchPredicateFactory> field(String fieldPath);

	/**
	 * Order elements by the value of a specific field.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param fieldReference The reference representing the <a href="#field-paths">path</a> to the index field to sort by.
	 * @return A DSL step where the "field" sort can be defined in more details.
	 * @throws SearchException If the field doesn't exist or cannot be sorted on.
	 */
	@Incubating
	<T> FieldSortOptionsGenericStep<NonStaticMetamodelScope, T, ?, ?, ? extends SearchPredicateFactory> field(
			FieldSortFieldReference<? super NonStaticMetamodelScope, T> fieldReference);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field
	 * containing the location to compute the distance from.
	 * @param location The location to which we want to compute the distance.
	 * @return A DSL step where the "distance" sort can be defined in more details.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	DistanceSortOptionsStep<NonStaticMetamodelScope, ?, ? extends SearchPredicateFactory> distance(String fieldPath,
			GeoPoint location);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param fieldReference The reference representing the <a href="#field-paths">path</a> to the index field
	 * containing the location to compute the distance from.
	 * @param location The location to which we want to compute the distance.
	 * @return A DSL step where the "distance" sort can be defined in more details.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	@Incubating
	default DistanceSortOptionsStep<NonStaticMetamodelScope, ?, ? extends SearchPredicateFactory> distance(
			DistanceSortFieldReference<? super NonStaticMetamodelScope> fieldReference, GeoPoint location) {
		return distance( fieldReference.absolutePath(), location );
	}

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field
	 * containing the location to compute the distance from.
	 * @param latitude The latitude of the location to which we want to compute the distance.
	 * @param longitude The longitude of the location to which we want to compute the distance.
	 * @return A DSL step where the "distance" sort can be defined in more details.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	default DistanceSortOptionsStep<NonStaticMetamodelScope, ?, ? extends SearchPredicateFactory> distance(String fieldPath,
			double latitude,
			double longitude) {
		return distance( fieldPath, GeoPoint.of( latitude, longitude ) );
	}

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param fieldReference The reference representing the <a href="#field-paths">path</a> to the index field
	 * containing the location to compute the distance from.
	 * @param latitude The latitude of the location to which we want to compute the distance.
	 * @param longitude The longitude of the location to which we want to compute the distance.
	 * @return A DSL step where the "distance" sort can be defined in more details.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	@Incubating
	default DistanceSortOptionsStep<NonStaticMetamodelScope, ?, ? extends SearchPredicateFactory> distance(
			DistanceSortFieldReference<? super NonStaticMetamodelScope> fieldReference, double latitude,
			double longitude) {
		return distance( fieldReference, GeoPoint.of( latitude, longitude ) );
	}

	/**
	 * Create a new sort factory whose root for all paths passed to the DSL
	 * will be the given object field.
	 * <p>
	 * This is used to call reusable methods that can apply the same sort
	 * on different object fields that have same structure (same sub-fields).
	 *
	 * @param objectFieldPath The path from the current root to an object field that will become the new root.
	 * @return A new sort factory using the given object field as root.
	 */
	@Incubating
	SearchSortFactory withRoot(String objectFieldPath);

	/**
	 * @param relativeFieldPath The path to a field, relative to the {@link #withRoot(String) root} of this factory.
	 * @return The absolute path of the field, for use in native sorts for example.
	 * Note the path is returned even if the field doesn't exist.
	 */
	@Incubating
	String toAbsolutePath(String relativeFieldPath);

}
