package com.hyp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "taxes")
public class Tax extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("tax_name")
    private String taxName;

    @Field("tax_tax_type")
    private String taxTaxType;

    @Field("tax_core_or_total")
    private String taxCoreOrTotal;

    @Field("rank")
    private String rank;

    @Field("description")
    private String description;

    @Field("active")
    private String active;

    @Field("tax")
    private String tax;

    @Field("consider_in_core_amount")
    private String considerInCoreAmount;

    @Field("tax_type")
    private String taxType;

    @Field("tax_order_type")
    private String taxOrderType;
}
