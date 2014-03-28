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
