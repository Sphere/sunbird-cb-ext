package org.sunbird.learnerPath.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sunbird.learnerPath.repository.model.UserPassbook;

import java.util.List;

@Repository
public interface UserPassbookRepository extends CassandraRepository<UserPassbook, String> {

    List<UserPassbook> findByUserid(String userid);

}
