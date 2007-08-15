//$Id$
package org.hibernate.search.test.session;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;

import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Indexed
@Entity
public class Entite {
	@DocumentId
	@Id
	@GeneratedValue
	private Integer id;

	@Field( index = Index.TOKENIZED, store = Store.YES )
	private String titre;

	@IndexedEmbedded
	@ManyToOne(fetch = FetchType.LAZY)
	private Categorie categorie;

	public Entite() {
	}

	public Entite(String titre, Categorie categorie) {
		this.titre = titre;
		this.categorie = categorie;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitre() {
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	public Categorie getCategorie() {
		return categorie;
	}

	public void setCategorie(Categorie categorie) {
		this.categorie = categorie;
	}
}