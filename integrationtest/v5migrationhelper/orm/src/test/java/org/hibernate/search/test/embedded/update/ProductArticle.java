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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

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
	@Field
	@IndexingDependency(derivedFrom = {
			@ObjectPath({@PropertyValue(propertyName = "model"),
					@PropertyValue(propertyName = "mainReferenceCode"),
					@PropertyValue(propertyName = "rawValue")}),
			@ObjectPath({@PropertyValue(propertyName = "model"),
					@PropertyValue(propertyName = "additionalReferenceCodes"),
					@PropertyValue(propertyName = "rawValue")})
	})
	public Collection<String> getProductReferenceCodeWithColorCollection() {
		Collection<String> productReferenceCodeWithColorCollection = new ArrayList<String>();

		productReferenceCodeWithColorCollection.add( getProductReferenceCodeWithColor( model.getMainReferenceCode() ) );
		for ( ProductReferenceCode code : model.getAdditionalReferenceCodes() ) {
			productReferenceCodeWithColorCollection.add( getProductReferenceCodeWithColor( code ) );
		}

		return Collections.<String>unmodifiableCollection( productReferenceCodeWithColorCollection );
	}

	@Transient
	private String getProductReferenceCodeWithColor(ProductReferenceCode referenceCode) {
		StringBuilder sb = new StringBuilder();
		sb.append( referenceCode.getRawValue() );
		sb.append( colorCode );
		return sb.toString();
	}
}
