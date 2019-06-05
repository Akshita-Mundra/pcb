/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sc.fe.analyze.data.repo;

import com.sc.fe.analyze.data.entity.FiletypeExtensions;
import java.util.List;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author pc
 */
@Repository
public interface FiletypeExtensionsRepo extends CassandraRepository<FiletypeExtensions, String> {

    List<FiletypeExtensions> findByExtensions(final String extension);
}
