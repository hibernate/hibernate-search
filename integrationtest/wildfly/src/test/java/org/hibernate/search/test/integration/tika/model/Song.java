/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.tika.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TikaBridge;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
public class Song {

	@Id
	@GeneratedValue
	long id;

	@Field
	@TikaBridge(metadataProcessor = Mp3TikaMetadataProcessor.class)
	String mp3FileName;

	public Song(String mp3FileName) {
		this.mp3FileName = mp3FileName;
	}

	public Song() {
	}

	public Long getId() {
		return id;
	}

	public String getMp3FileName() {
		return mp3FileName;
	}

	public void setMp3FileName(String mp3FileName) {
		this.mp3FileName = mp3FileName;
	}
}
