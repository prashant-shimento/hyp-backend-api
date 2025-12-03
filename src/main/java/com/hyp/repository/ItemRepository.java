package com.hyp.repository;

import com.hyp.entity.Item;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemRepository extends MongoRepository<Item, String> {

    List<Item> findAllByIdIn(List<String> requestItemIds);
}
