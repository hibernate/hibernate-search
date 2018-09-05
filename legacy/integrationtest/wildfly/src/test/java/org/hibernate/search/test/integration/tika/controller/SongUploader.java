/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.tika.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.integration.tika.model.Song;

/**
 * @author Davide D'Alto
 */
@Stateful
@Model
public class SongUploader {

	@Inject
	private FullTextEntityManager em;

	private Song newSong;

	@Produces
	@Named
	public Song getNewSong() {
		return newSong;
	}

	public void upload() throws Exception {
		upload( newSong );
	}

	public void upload(Song song) throws Exception {
		em.persist( song );
		initNewSong();
	}

	@SuppressWarnings("unchecked")
	public List<Song> search(String name) {
		QueryBuilder queryBuilder = em.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Song.class )
				.get();
		Query luceneQuery = queryBuilder
				.keyword()
				.onField( "mp3FileName" )
				.ignoreFieldBridge()
				.matching( name )
				.createQuery();

		return em.createFullTextQuery( luceneQuery ).getResultList();
	}

	@PostConstruct
	public void initNewSong() {
		newSong = new Song();
	}

}
