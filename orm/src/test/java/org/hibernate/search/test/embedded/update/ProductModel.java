/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
