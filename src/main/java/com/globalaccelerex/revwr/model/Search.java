package com.globalaccelerex.revwr.model;

import javax.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

/**
 *
 * @author Abiola.Adebanjo
 */
public class Search {

    @URL
    @NotEmpty
    private String link;

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

}
