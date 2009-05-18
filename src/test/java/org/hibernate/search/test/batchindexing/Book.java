package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;

@Indexed
@Entity
public class Book {
	
	@Id
	public long id;

}
