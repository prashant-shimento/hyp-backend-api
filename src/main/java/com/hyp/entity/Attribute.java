package com.hyp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attributes")
public class Attribute extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String attribute;

    private String active;
}
