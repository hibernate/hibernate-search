/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.repository;

import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;

import org.springframework.data.repository.CrudRepository;

public interface DocumentCopyRepository extends CrudRepository<DocumentCopy<?>, Integer> {
}
