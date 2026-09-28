package org.hibernate.search.integrationtest.showcase.library.repository;

import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;

import org.springframework.data.repository.CrudRepository;

public interface BorrowalRepository extends CrudRepository<Borrowal, Integer> {
}
