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
