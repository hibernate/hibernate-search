/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import java.util.Set;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface MassIndexingTypeGroupMonitorContext {

	Set<MassIndexingType> includedTypes();

}
