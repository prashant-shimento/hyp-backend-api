package com.hyp.repository;

import com.hyp.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {

    Optional<Permission> findByName(String name);

    List<Permission> findByActiveTrue();

    List<Permission> findByCategory(String category);

    List<Permission> findByCategoryAndActiveTrue(String category);

    List<Permission> findByIsSystemTrue();

    boolean existsByName(String name);

    List<Permission> findByNameIn(List<String> names);
}
