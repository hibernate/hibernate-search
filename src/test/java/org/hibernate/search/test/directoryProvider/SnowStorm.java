//$Id$
package org.hibernate.search.test.directoryProvider;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;

/**
 * @author Emmanuel Bernard
 */
@Indexed
@Entity
public class SnowStorm {
	@Id
	@GeneratedValue
	@DocumentId
	private Long id;

	@Field(index = Index.UN_TOKENIZED)
	@DateBridge( resolution = Resolution.DAY )
	@Column(name="xdate")
	private Date date;

	@Field(index = Index.TOKENIZED)
	private String location;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
