package com.hyp.repository;

import com.hyp.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    Optional<Role> findByName(String name);

    List<Role> findByActiveTrue();

    List<Role> findByIsSystemTrue();

    boolean existsByName(String name);
}
