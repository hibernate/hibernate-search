//$Id$
package org.hibernate.search.test.session;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Indexed
@Entity
public class Categorie {

	@DocumentId
	@Id @GeneratedValue
	private Integer id;

	@Field( index = Index.TOKENIZED, store = Store.YES )
	private String nom;

	public Categorie() {
	}

	public Categorie(String nom) {
		this.nom = nom;
	}

	public String toString() {
		return ( nom );
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}
}


