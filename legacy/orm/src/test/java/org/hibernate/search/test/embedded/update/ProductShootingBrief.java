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
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.embedded.fieldoncollection.CollectionOfStringsFieldBridge;

@Entity
@Indexed
public class ProductShootingBrief {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@OneToMany(mappedBy = "shootingBrief", fetch = FetchType.LAZY)
	private Set<ProductArticle> articles = new HashSet<ProductArticle>();

	@OneToMany(mappedBy = "shootingBrief", fetch = FetchType.LAZY)
	private Set<ProductModel> models = new HashSet<ProductModel>();

	protected ProductShootingBrief() {
	}

	public ProductShootingBrief(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<ProductArticle> getArticles() {
		return articles;
	}

	public void setArticles(Set<ProductArticle> articles) {
		this.articles = articles;
	}

	public Set<ProductModel> getModels() {
		return models;
	}

	public void setModels(Set<ProductModel> models) {
		this.models = models;
	}

	@Transient
	@Field(bridge = @FieldBridge(impl = CollectionOfStringsFieldBridge.class), analyzer = @Analyzer(impl = StandardAnalyzer.class))
	public Collection<String> getReferenceCodeCollection() {
		Collection<String> referenceCodes = new ArrayList<String>();

		for ( ProductArticle article : articles ) {
			referenceCodes.addAll( article.getProductReferenceCodeWithColorCollection() );
		}
		for ( ProductModel model : models ) {
			referenceCodes.addAll( model.getProductReferenceCodeCollection() );

			for ( ProductArticle article : model.getArticles() ) {
				if ( article.getShootingBrief() == null ) {
					referenceCodes.addAll( article.getProductReferenceCodeWithColorCollection() );
				}
			}
		}

		return Collections.unmodifiableCollection( referenceCodes );
	}

}
