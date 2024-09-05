/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface MassIndexingType {

	String entityName();

}
