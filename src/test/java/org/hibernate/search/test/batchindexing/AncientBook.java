package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class AncientBook extends Book {
	
	public String catalogueGroupName = "";

}
