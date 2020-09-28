/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.search.integrationtest.batch.jsr352.massindexing.id.EmbeddableDateId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * @author Mincong Huang
 */
@Entity
@Indexed
public class EntityWithEmbeddedId {

	@EmbeddedId
	@DocumentId
	private EmbeddableDateId embeddableDateId;

	@GenericField
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
