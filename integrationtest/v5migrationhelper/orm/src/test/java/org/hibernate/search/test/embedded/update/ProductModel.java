/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.embedded.fieldoncollection.CollectionOfStringsFieldBridge;

@Entity
@Indexed
public class ProductModel {

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne(cascade = CascadeType.ALL)
	private ProductReferenceCode mainReferenceCode;

	@OneToMany(mappedBy = "model", cascade = CascadeType.ALL)
	private List<ProductReferenceCode> additionalReferenceCodes = new ArrayList<ProductReferenceCode>();

	@OneToMany(mappedBy = "model", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ContainedIn
	private Set<ProductArticle> articles = new HashSet<ProductArticle>();

	@ManyToOne(fetch = FetchType.LAZY)
	@ContainedIn
	private ProductShootingBrief shootingBrief;

	protected ProductModel() {
	}

	public ProductModel(String referenceCode) {
		this.mainReferenceCode = new ProductReferenceCode( this, referenceCode );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ProductReferenceCode getMainReferenceCode() {
		return mainReferenceCode;
	}

	public void setMainReferenceCode(ProductReferenceCode mainReferenceCode) {
		this.mainReferenceCode = mainReferenceCode;
	}

	public List<ProductReferenceCode> getAdditionalReferenceCodes() {
		return additionalReferenceCodes;
	}

	public void setAdditionalReferenceCodes(List<ProductReferenceCode> additionalReferenceCodes) {
		this.additionalReferenceCodes = additionalReferenceCodes;
	}

	public Set<ProductArticle> getArticles() {
		return articles;
	}

	public void setArticles(Set<ProductArticle> articles) {
		this.articles = articles;
	}

	public ProductShootingBrief getShootingBrief() {
		return shootingBrief;
	}

	public void setShootingBrief(ProductShootingBrief shootingBrief) {
		if ( shootingBrief != null ) {
			shootingBrief.getModels().add( this );
		}
		this.shootingBrief = shootingBrief;
	}

	@Field(bridge = @FieldBridge(impl = CollectionOfStringsFieldBridge.class))
	public Collection<String> getProductReferenceCodeCollection() {
		Collection<String> productReferenceCodeCollection = new ArrayList<String>();

		productReferenceCodeCollection.add( mainReferenceCode.getRawValue() );
		for ( ProductReferenceCode code : additionalReferenceCodes ) {
			productReferenceCodeCollection.add( code.getRawValue() );
		}

		return Collections.<String>unmodifiableCollection( productReferenceCodeCollection );
	}

}
