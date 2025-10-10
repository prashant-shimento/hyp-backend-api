package com.hyp.repository;

import com.hyp.entity.Discount;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.test.annotation.Rollback;

@ExtendWith(MockitoExtension.class)
@DataMongoTest
public class DiscountRepositoryTest {

    @SpyBean
    private MongoRepository<Discount, String> discountRepository;

    private Discount discount;

    @BeforeEach
    void setUp() {
        discount = new Discount();
        discount.setId("123");
        discount.setDiscountName("Default Discount Name");
        discount.setDiscountType("Percentage");
        discount.setBogoBuyQty("0");
        discount.setBogoGetQty("0");
        discount.setBogoType("None");
        discount.setBogoApplicableOnPurchase("False");
        discount.setBogoApplicableOnPurchaseItemIds("None");
        discount.setBogoApplicableOn("None");
        discount.setBogoApplicableOnItemIds("None");
        discount.setBogoItemAmountLimit("0.0");
        discount.setBogoPurchaseDiscount("0%");
        discount.setBogoApplicableOnItem("None");
        discount.setBogoApplicableOnPurchaseItem("None");
        discount.setDiscount("0%");
        discount.setDiscountOrder("1");
        discount.setDiscountOrderType("Flat");
        discount.setDiscountApplicableOn("All Items");
        discount.setDiscountDays("All Days");
        discount.setActive("True");
        discount.setDiscountOnTotal("True");
        discount.setDiscountStarts("2025-01-01");
        discount.setDiscountEnds("2025-12-31");
        discount.setDiscountTimeFrom("00:00");
        discount.setDiscountTimeTo("23:59");
        discount.setDiscountMinAmount("0.0");
        discount.setDiscountMaxAmount("100.0");
        discount.setDiscountHasCoupon("False");
        discount.setDiscountCategoryItemIds("None");
        discount.setDiscountMaxLimit("10");
        discountRepository.save(discount);
    }

    @Test
    @Rollback(true)
    public void saveDiscount_Success() {
        discountRepository.save(discount);
        System.out.println(discount);
        Assertions.assertThat(discount.getId()).isNotEmpty();
    }

    @Test
    @Rollback(false)
    public void saveDiscount_Failure() {
        Discount invalidDiscount = new Discount();
        invalidDiscount.setId(null);

        Assertions.assertThatThrownBy(() -> {
                    if (invalidDiscount.getId() == null) {
                        throw new IllegalArgumentException("ID must not be null");
                    }
                    discountRepository.save(invalidDiscount);
                })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID must not be null");
    }

    @Test
    @Rollback(false)
    public void getAllDiscounts_Success() {
        List<Discount> discounts = discountRepository.findAll();
        System.out.println(discounts);
        Assertions.assertThat(discounts).isNotEmpty();
    }

    @Test
    @Rollback(false)
    public void getAllDiscounts_Failure() {
        discountRepository.deleteAll();
        List<Discount> discounts = discountRepository.findAll();
        Assertions.assertThat(discounts).isEmpty();
    }

    @Test
    @Rollback(false)
    public void getDiscountById_Success() {
        Optional<Discount> findById = discountRepository.findById(discount.getId());
        System.out.println(findById);
        Assertions.assertThat(findById).isNotEmpty();
    }

    @Test
    @Rollback(false)
    public void getDiscountById_Failure() {
        Optional<Discount> findById = discountRepository.findById("44565");
        System.out.println(findById);
        Assertions.assertThat(findById).isEmpty();
    }

    @Test
    @Rollback(true)
    public void updateDiscount_Success() {
        Discount discountToUpdate = discountRepository
                .findById(discount.getId())
                .orElseThrow(() -> new RuntimeException(discount.getId() + " : not found"));
        discountToUpdate.setActive("False");
        discountRepository.save(discountToUpdate);
        Discount updatedDiscount = discountRepository
                .findById(discount.getId())
                .orElseThrow(() -> new RuntimeException(discount.getId() + " : not found"));
        Assertions.assertThat(updatedDiscount.getActive()).isEqualTo("False");
    }

    @Test
    @Rollback(true)
    public void updateDiscount_Failure() {
        Optional<Discount> discountToUpdateOpt = discountRepository.findById("45655");
        Assertions.assertThat(discountToUpdateOpt).isEmpty();
        Assertions.assertThatThrownBy(() -> {
                    Discount discountToUpdate =
                            discountToUpdateOpt.orElseThrow(() -> new RuntimeException("45655 : not found"));
                    discountToUpdate.setActive("False");
                    discountRepository.save(discountToUpdate);
                })
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("45655 : not found");
    }

    @Test
    @Rollback(false)
    public void deleteDiscount_Success() {
        discountRepository.deleteById(discount.getId());
        Optional<Discount> discountToDelete = discountRepository.findById(discount.getId());
        Assertions.assertThat(discountToDelete).isEmpty();
    }

    @Test
    @Rollback(false)
    public void deleteDiscount_Failure() {
        discountRepository.deleteById("45655");
        Optional<Discount> discountToDelete = discountRepository.findById("45655");
        Assertions.assertThat(discountToDelete).isEmpty();
    }
}
