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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.embedded.fieldoncollection.CollectionOfStringsFieldBridge;

@Entity
@Indexed
public class ProductArticle {

	@Id
	@GeneratedValue
	private Long id;

	private String colorCode;

	@ManyToOne(optional = false)
	// Note: we have a custom fieldBridge here, that's why there's not @IndexedEmbedded even if we also search on
	// the ProductModel information
	private ProductModel model;

	@ManyToOne(fetch = FetchType.LAZY)
	@ContainedIn
	private ProductShootingBrief shootingBrief;

	protected ProductArticle() {
	}

	public ProductArticle(ProductModel model, String colorCode) {
		this.model = model;
		this.colorCode = colorCode;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
	}

	public ProductModel getModel() {
		return model;
	}

	public void setModel(ProductModel model) {
		this.model = model;
	}

	public ProductShootingBrief getShootingBrief() {
		return shootingBrief;
	}

	public void setShootingBrief(ProductShootingBrief shootingBrief) {
		if ( shootingBrief != null ) {
			shootingBrief.getArticles().add( this );
		}
		this.shootingBrief = shootingBrief;
	}

	@Transient
	@Field(bridge = @FieldBridge(impl = CollectionOfStringsFieldBridge.class))
	public Collection<String> getProductReferenceCodeWithColorCollection() {
		Collection<String> productReferenceCodeWithColorCollection = new ArrayList<String>();

		productReferenceCodeWithColorCollection.add( getProductReferenceCodeWithColor( model.getMainReferenceCode() ) );
		for ( ProductReferenceCode code : model.getAdditionalReferenceCodes() ) {
			productReferenceCodeWithColorCollection.add( getProductReferenceCodeWithColor( code ) );
		}

		return Collections.<String>unmodifiableCollection( productReferenceCodeWithColorCollection );
	}

	@Transient
	@ContainedIn
	private ProductShootingBrief getModelShootingBrief() {
		return model.getShootingBrief();
	}

	@Transient
	private String getProductReferenceCodeWithColor(ProductReferenceCode referenceCode) {
		StringBuilder sb = new StringBuilder();
		sb.append( referenceCode.getRawValue() );
		sb.append( colorCode );
		return sb.toString();
	}
}
