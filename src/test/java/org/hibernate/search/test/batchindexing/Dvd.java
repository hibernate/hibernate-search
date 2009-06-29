package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Indexed
@Entity
public class Dvd implements TitleAble {
	
	public long unusuallyNamedIdentifier;
	public String title;

	@Id
	@GeneratedValue
	public long getUnusuallyNamedIdentifier() {
		return unusuallyNamedIdentifier;
	}

	public void setUnusuallyNamedIdentifier(long unusuallyNamedIdentifier) {
		this.unusuallyNamedIdentifier = unusuallyNamedIdentifier;
	}

	@Field
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
}
