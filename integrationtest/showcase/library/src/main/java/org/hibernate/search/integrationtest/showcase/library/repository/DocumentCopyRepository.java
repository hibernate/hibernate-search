package org.hibernate.search.integrationtest.showcase.library.repository;

import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;

import org.springframework.data.repository.CrudRepository;

public interface DocumentCopyRepository extends CrudRepository<DocumentCopy<?>, Integer> {
}
