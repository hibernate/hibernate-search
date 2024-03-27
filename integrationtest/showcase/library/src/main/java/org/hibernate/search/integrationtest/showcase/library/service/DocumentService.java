/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.repository.DocumentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentService {

	@Autowired
	private DocumentRepository documentRepo;

	public Book createBook(int id, String isbn, String title, String author, String summary, String tags) {
		return documentRepo.save( new Book( id, isbn, title, author, summary, tags ) );
	}

	public Video createVideo(int id, String title, String author, String summary, String tags) {
		return documentRepo.save( new Video( id, title, author, summary, tags ) );
	}

	public long count() {
		return documentRepo.count();
	}

	public long countIndexed() {
		return documentRepo.countIndexed();
	}

	public List<Document<?>> findAllIndexed() {
		return documentRepo.findAllIndexed();
	}

	public Optional<Book> getByIsbn(String isbnAsString) {
		return documentRepo.getByIsbn( isbnAsString );
	}

	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		return documentRepo.searchByMedium( terms, medium, offset, limit );
	}

	public List<Document<?>> searchAroundMe(String terms, String tags, GeoPoint myLocation, Double maxDistanceInKilometers,
			List<LibraryServiceOption> options,
			int offset, int limit) {
		return documentRepo.searchAroundMe( terms, tags, myLocation, maxDistanceInKilometers, options, offset, limit );
	}

	public List<String> getAuthorsOfBooksHavingTerms(String terms, SortOrder order) {
		return documentRepo.getAuthorsOfBooksHavingTerms( terms, order );
	}

	public void purge() {
		documentRepo.deleteAll();
		documentRepo.purge();
	}
}
