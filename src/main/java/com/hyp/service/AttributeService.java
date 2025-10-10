package com.hyp.service;

import com.hyp.entity.Attribute;
import com.hyp.repository.AttributeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttributeService extends BaseServiceImpl<Attribute, String> {
    @Autowired
    AttributeRepository attributeRepository;
}
