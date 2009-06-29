package org.hibernate.search.test.batchindexing;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class AncientBook extends Book {
	
	public String catalogueGroupName = "";

	public String getCatalogueGroupName() {
		return catalogueGroupName;
	}

	public void setCatalogueGroupName(String catalogueGroupName) {
		this.catalogueGroupName = catalogueGroupName;
	}
	
}
