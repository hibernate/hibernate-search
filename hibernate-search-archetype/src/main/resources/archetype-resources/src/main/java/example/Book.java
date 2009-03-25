package example;

import org.hibernate.search.annotations.*;
import org.apache.solr.analysis.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import example.Author;

/**
 *
 */
@Entity
@AnalyzerDef(name = "customanalyzer",
		tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
		filters = {
				@TokenFilterDef(factory = LowerCaseFilterFactory.class),
				@TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = {
						@Parameter(name = "language", value = "English")
				})
		})
@Indexed
public class Book {

    private Integer id;
    private String title;
    private String subtitle;
    private Set<Author> authors = new HashSet<Author>();
    private Date publicationDate;

    @IndexedEmbedded
    @ManyToMany
    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    public Book() {
    }

    @Field(index = Index.TOKENIZED, store = Store.YES)
    @Analyzer(definition = "customanalyzer")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Id
    @DocumentId
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Field(index = Index.TOKENIZED, store = Store.NO)
    @Analyzer(definition = "customanalyzer")
    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    @DateBridge(resolution = Resolution.DAY)
    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }
}
