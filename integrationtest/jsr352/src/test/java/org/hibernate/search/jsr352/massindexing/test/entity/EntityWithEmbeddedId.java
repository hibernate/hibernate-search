/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jsr352.massindexing.test.bridge.DateIdBridge;
import org.hibernate.search.jsr352.massindexing.test.id.EmbeddableDateId;

/**
 * @author Mincong Huang
 */
@Entity
@Indexed
public class EntityWithEmbeddedId {

	@EmbeddedId
	@DocumentId
	@FieldBridge(impl = DateIdBridge.class)
	private EmbeddableDateId embeddableDateId;

	@Field
	private String value;

	public EntityWithEmbeddedId() {
	}

	public EntityWithEmbeddedId(LocalDate d) {
		this.embeddableDateId = new EmbeddableDateId( d );
		this.value = DateTimeFormatter.ofPattern( "yyyyMMdd", Locale.ROOT ).format( d );
	}

	public EmbeddableDateId getEmbeddableDateId() {
		return embeddableDateId;
	}

	public void setEmbeddableDateId(EmbeddableDateId embeddableDateId) {
		this.embeddableDateId = embeddableDateId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "MyDate [embeddableDateId=" + embeddableDateId + ", value=" + value + "]";
	}

}
