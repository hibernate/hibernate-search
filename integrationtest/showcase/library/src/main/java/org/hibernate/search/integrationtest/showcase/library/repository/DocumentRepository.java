package org.hibernate.search.integrationtest.showcase.library.repository;

import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.repository.indexsearch.IndexSearchDocumentRepository;

import org.springframework.data.repository.CrudRepository;

public interface DocumentRepository extends CrudRepository<Document<?>, Integer>, IndexSearchDocumentRepository {
}
