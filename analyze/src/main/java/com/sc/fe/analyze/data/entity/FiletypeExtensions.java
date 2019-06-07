/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sc.fe.analyze.data.entity;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 *
 * @author pc
 */
@Table(value = "filetype_extensions")
public class FiletypeExtensions implements Serializable {

    @PrimaryKey
    private FiletypeExtensionsPK key;
    private Set<String> extensions;

    public FiletypeExtensionsPK getKey() {
        return key;
    }

    public void setKey(FiletypeExtensionsPK key) {
        this.key = key;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Set<String> extensions) {
        this.extensions = extensions;
    }

    public UUID getId() {
        return getKey().getId();
    }

    public void setId(UUID id) {
        this.getKey().setId(id);
    }

}
