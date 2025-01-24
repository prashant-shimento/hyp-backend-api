package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Discount;

@ExtendWith(MockitoExtension.class)
public class DiscountServiceTest {

	@Mock
	private MongoRepository<Discount, String> discountRepository;

	@InjectMocks
	private BaseServiceImpl<Discount, String> discountService = new BaseServiceImpl<>() {
	};

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
	}

	@Test
	public void saveDiscount_Success() {
		when(discountRepository.save(discount)).thenReturn(discount);
		discount = discountService.save(discount);
		assertNotNull(discount);
		assertEquals("Default Discount Name", discount.getDiscountName());
		assertEquals("True", discount.getActive());
		assertEquals("None", discount.getBogoApplicableOn());
		verify(discountRepository, times(1)).save(discount);
	}

	@Test
	public void findAllDiscounts_Success() {
		List<Discount> mockDiscounts = Arrays.asList(new Discount(), new Discount());
		when(discountRepository.findAll()).thenReturn(mockDiscounts);
		List<Discount> discounts = discountService.findAll();
		assertNotNull(discounts);
		assertEquals(2, discounts.size());
		verify(discountRepository, times(1)).findAll();
	}

	@Test
	public void findDiscountById_Success() {
		String id = "123";
		when(discountRepository.findById(id)).thenReturn(Optional.of(discount));
		Discount discount = discountService.findById(id);
		assertNotNull(discount);
		assertEquals("123", discount.getId());
		verify(discountRepository, times(1)).findById("123");
	}

	@Test
	public void findDiscountById_Failure() {
		String id = "12345";
		when(discountRepository.findById(id)).thenReturn(Optional.empty());
		Discount discount = discountService.findById(id);
		assertNull(discount);
		verify(discountRepository, times(1)).findById("12345");
	}

	@Test
	public void updateDiscount_Success() {
		String id = "123";
		when(discountRepository.findById(id)).thenReturn(Optional.of(discount));
		Discount existingDiscount = discountService.findById(id);
		assertNotNull(discount);
		existingDiscount.setBogoApplicableOn("Yes");
		discountService.update(discount);
		verify(discountRepository, times(1)).findById("123");
		verify(discountRepository, times(1)).save(existingDiscount);
	}

	@Test
	public void updateDiscount_Failure() {
		String id = "12345";
		when(discountRepository.findById(id)).thenReturn(Optional.empty());
		Discount existingDiscount = discountService.findById(id);
		assertNull(existingDiscount);
		verify(discountRepository, times(1)).findById("12345");
		verify(discountRepository, times(0)).save(any(Discount.class));
	}

	@Test
	public void deleteDiscount_Success() {
		String id = "123";
		doNothing().when(discountRepository).deleteById(id);
		discountService.deleteById(id);
		verify(discountRepository, times(1)).deleteById(id);
	}

	@Test
	public void deleteDiscount_Failure() {
		String id = "321";
		lenient().when(discountRepository.findById(id)).thenReturn(Optional.empty());
		doNothing().when(discountRepository).deleteById(id);
		discountService.deleteById(id);
		verify(discountRepository, times(1)).deleteById(id);
	}
}
