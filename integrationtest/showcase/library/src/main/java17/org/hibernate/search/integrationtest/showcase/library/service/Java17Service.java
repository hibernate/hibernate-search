/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.hibernate.search.integrationtest.showcase.library.dto.LibrarySimpleProjectionRecord;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Java17Service {

	@Autowired
	private EntityManager entityManager;

	public List<LibrarySimpleProjectionRecord> searchAndProjectToRecord(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.session( entityManager )
				.search( Library.class )
				.select( LibrarySimpleProjectionRecord.class )
				.where( f -> f.match().field( "name" ).matching( terms ) )
				.sort( f -> f.field( "collectionSize" ).desc()
						.then().field( "name_sort" ) )
				.fetchHits( offset, limit );
	}

	public List<LibrarySimpleProjectionRecord> searchAndProjectToMethodLocalRecord(String terms, int offset, int limit) {
		@ProjectionConstructor
		record LocalRecord(String name, List<LibraryServiceOption> services) { }

		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.session( entityManager )
				.search( Library.class )
				.select( LocalRecord.class )
				.where( f -> f.match().field( "name" ).matching( terms ) )
				.sort( f -> f.field( "collectionSize" ).desc()
						.then().field( "name_sort" ) )
				.fetchHits( offset, limit )
				.stream()
				.map( local -> new LibrarySimpleProjectionRecord( local.name, local.services ) )
				.collect( Collectors.toList() );
	}

}
