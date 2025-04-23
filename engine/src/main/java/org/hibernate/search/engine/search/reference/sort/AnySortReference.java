/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference.sort;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A generic sort-field reference that can be manually created and passed to any sort-DSL methods that require a field reference.
 * <p>
 * While it is expected that the generated Hibernate Search static metamodel will contain
 * more precise field references that would match the actual field capabilities, this generic reference can
 * be used by the users if they decide to opt-out of using the generated static metamodel
 * and would just want to create a few simple pre-defined references.
 *
 * @param absolutePath The absolut field path.
 * @param scopeRootType The class representing the scope root type.
 * @param valueModel The model of sort values. See {@link ValueModel}.
 * @param sortType The class representing the type of the sorted field, as per {@code valueModel}.
 * @param <SR> Scope root type
 * @param <T> The type of the sorted field.
 */
@Incubating
public record AnySortReference<SR, T>(String absolutePath, Class<SR> scopeRootType, ValueModel valueModel, Class<T> sortType)
		implements DistanceSortFieldReference<SR>,
		FieldSortFieldReference<SR, T>,
		ScoreSortFieldReference<SR, T> {
}
