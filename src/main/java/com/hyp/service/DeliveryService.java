package com.hyp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.client.PidgeClient;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryOrderStatus.DeliveryOrderData;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryQuote.DeliveryNetworks;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.repository.DeliveryRepository;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeliveryService extends BaseServiceImpl<Delivery, String> {

	@Value("${delivery.pidge.url}")
	private String baseUrl;

	@Value("${delivery.pidge.smart.id}")
	private Integer smartId;

	@Value("${delivery.pidge.username}")
	private String pidgeUsername;

	@Value("${delivery.pidge.password}")
	private String pidgePassword;

	@Autowired
	DeliveryRepository deliveryRepository;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	ApiLogService apiRequestResponseLogService;

	@Autowired
	RetryTemplate retryTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	PosService posService;

	@Autowired
	OrderService orderService;

	@Autowired
	PidgeClient pidgeClient;

	@Autowired
	NotificationService notificationService;

	@Autowired
	RedisService redisService;

	@Autowired
	CustomerService customerService;

	@Autowired
	AddressService addressService;

	@Autowired
	private OrderEventPublisher orderEventPublisher;

	public Delivery findByOrderId(String orderId) {
		return deliveryRepository.findByOrderIdAndIsDeletedFalse(orderId);
	}

	public Delivery findByDeliveryOrderId(String deliveryOrderId) {
		return deliveryRepository.findByDeliveryOrderIdAndIsDeletedFalse(deliveryOrderId);
	}

	public void proceesDeliveryOrder(Order order) {
		try {
			Customer customer = customerService.findById(order.getCustomerId());
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			Address address = addressService.findById(order.getDeliveryDetails().getAddressId());
			DeliveryOrderRequest deliveryOrderRequest = DeliveryRequestTranslation.getDeliveryOrderRequest(restaurant,
					address, customer, order);
			createOrder(deliveryOrderRequest, order);
		} catch (DeliveryException e) {
			log.error("Error occured while proceesDeliveryOrder for orderId {} cause: ", order.getId(), e.getMessage());
		}
	}

	public void createOrder(DeliveryOrderRequest deliveryOrderRequest, Order order) throws DeliveryException {
		pidgeClient.createDeliveryOrder(deliveryOrderRequest).flatMap(deliveryOrderId -> {
			Delivery delivery = DeliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
			delivery.setId(CommonUtils.genId());
			delivery.setDeliveryOrderId(deliveryOrderId);
			delivery.setStatus(DeliveryOrderStatusType.PENDING);
			delivery.setService(order.getDeliveryDetails().getService());
			delivery.setNetworkId(order.getDeliveryDetails().getNetworkId());
			delivery.setPickupNow(order.getDeliveryDetails().isPickupNow());
			return Mono.fromRunnable(() -> save(delivery));
		}).block();
	}

	public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
		return pidgeClient.getDeliveryQuote(deliveryQuoteRequest);
	}

	public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
		return pidgeClient.getServiceability(deliveryOrderId);
	}

	public void processDeliveryCallback(Delivery delivery, DeliveryOrderData deliveryOrderData)
			throws DeliveryException {
		try {

			DeliveryOrderStatusType status = DeliveryOrderStatusType
					.getDeliveryOrderStatus(deliveryOrderData.getStatus());
			delivery.setStatus(status);

			Order order = orderService.findById(delivery.getOrderId());

			if (delivery.getStatus() == DeliveryOrderStatusType.CANCELLED) {
				orderService.updateOrderStatus(order.getId(),
						OrderStatusType.getOrderStatusByDelvieryStatus(DeliveryFulfillStatusType.CANCELLED));
				orderEventPublisher.publishDeliveryEvent(delivery);
				return;
			}
			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED
					|| delivery.getStatus() == DeliveryOrderStatusType.COMPLETED) {
				handleFulfillmentStatus(delivery, deliveryOrderData, order);
			}
			save(delivery);
		} catch (Exception e) {
			handleDeliveryError("processDeliveryCallback", delivery, e);
		}
	}

	private void handleFulfillmentStatus(Delivery delivery, DeliveryOrderData deliveryOrderData, Order order) {
		DeliveryFulfillment deliveryFulfill = deliveryOrderData.getFulfillment();
		DeliveryFulfillStatusType fullFillStatus = deliveryFulfill.getStatus();

		if (fullFillStatus.equals(DeliveryFulfillStatusType.OUT_FOR_PICKUP)
				|| fullFillStatus.equals(DeliveryFulfillStatusType.CREATED)) {
			String redisKey = "delivery:" + delivery.getOrderId() + ":" + fullFillStatus;
			redisService.setRedisData(redisKey, fullFillStatus, Duration.ofMinutes(25).toSeconds());
		}

		delivery.setNetworkId(Integer.parseInt(deliveryFulfill.getChannel().getId()));
		delivery.setService(deliveryFulfill.getChannel().getName());
		delivery.setPickupNow(true);
		delivery.setFulfillment(deliveryFulfill);

		if (deliveryOrderData.getFulfillment().getTrackCode() != null) {
			delivery.getFulfillment().setTrackCode(deliveryOrderData.getFulfillment().getTrackCode());
			order.setDeliveryTrackingLink("https://t.pidge.in?t=" + delivery.getFulfillment().getTrackCode());
		}

		orderService.save(order);
		orderService.updateOrderStatus(order.getId(), OrderStatusType.getOrderStatusByDelvieryStatus(fullFillStatus));

		if (posService.isPosUpdateRequired(fullFillStatus)) {
			posService.updatePosRiderStatus(delivery, order);
		}
	}

	public void processDeliveryOrderFulfill(Delivery delivery, String fulfilledBy, String fulfillType)
			throws DeliveryException {
		try {
			if (delivery == null) {
				log.error("Cannot process fulfillment: Delivery object is null.");
				return;
			}
			if (!DeliveryOrderStatusType.PENDING.equals(delivery.getStatus())) {
				log.error("Cannot process fulfillment: Delivery status is not PENDING for delivery ID {}",
						delivery.getId());
				return;
			}
			if ("smart".equalsIgnoreCase(fulfillType)) {
				processDeliverySmartFulfill(delivery, fulfilledBy);
			} else {
				processDeliveryStandardFulfill(delivery, fulfilledBy);
			}
		} catch (Exception e) {
			handleDeliveryError("processDeliveryOrderFulfill", delivery, e);
		}

	}

	public void processDeliveryStandardFulfill(Delivery delivery, String fulfilledBy) throws DeliveryException {
		DeliveryNetworks selectedNetwork = getServicabilityToken(delivery);
		if (selectedNetwork != null) {
			String token = selectedNetwork.getToken();
			delivery.setNetworkToken(token);
			delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
			delivery.setNetworkId(selectedNetwork.getNetworkId());
			delivery.setService(selectedNetwork.getService());
			delivery.setPickupNow(selectedNetwork.isPickupNow());
			delivery.setFulfillmentType(fulfilledBy);
			delivery.setFulfillmentAt(LocalDateTime.now());
			save(delivery);
			pidgeClient.fulfillDeliveryOrder(DeliveryRequestTranslation.getOrderFulfillRequest(delivery)).subscribe();
		} else {
			throw new DeliveryException(
					"No matching network found with the specified networkId or minimum price network");
		}
	}

	public DeliveryNetworks getServicabilityToken(Delivery delivery) throws DeliveryException {
		DeliveryNetworks selectedNetwork = null;
		try {
			DeliveryQuote deliveryQuote = this.getServiceability(delivery.getDeliveryOrderId());
			List<DeliveryNetworks> deliveryNetworks = deliveryQuote.getData().getItems().stream()
					.filter(items -> items.isPickupNow())
					.filter(items -> !items.getService().equalsIgnoreCase("loadshare")).collect(Collectors.toList());

			Optional<DeliveryNetworks> matchingNetworkOpt = deliveryNetworks.stream()
					.filter(network -> network.getNetworkId() == delivery.getNetworkId()).findFirst();

			Optional<DeliveryNetworks> minPriceNetworkOpt = deliveryNetworks.stream()
					.filter(network -> network.getQuote() != null)
					.min(Comparator.comparingDouble(network -> network.getQuote().getPrice()));

			if (matchingNetworkOpt.isPresent() && minPriceNetworkOpt.isPresent()) {
				DeliveryNetworks matchingNetwork = matchingNetworkOpt.get();
				DeliveryNetworks minPriceNetwork = minPriceNetworkOpt.get();
				selectedNetwork = (minPriceNetwork.getQuote().getPrice() < matchingNetwork.getQuote().getPrice())
						? minPriceNetwork
						: matchingNetwork;
			} else if (matchingNetworkOpt.isPresent()) {
				selectedNetwork = matchingNetworkOpt.get();
			} else if (minPriceNetworkOpt.isPresent()) {
				selectedNetwork = minPriceNetworkOpt.get();
			}

		} catch (Exception e) {
			handleDeliveryError("getServicabilityToken", delivery, e);
		}
		return selectedNetwork;
	}

	public void processDeliverySmartFulfill(Delivery delivery, String fulfilledBy) throws DeliveryException {
		try {
			pidgeClient.smartFulfillDeliveryOrder(DeliveryRequestTranslation.getSmartFulfillRequest(delivery)).subscribe();
			delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
			delivery.setFulfillmentType(fulfilledBy);
			delivery.setFulfillmentAt(LocalDateTime.now());
			save(delivery);
		} catch (DeliveryException e) {
			handleDeliveryError("processDeliverySmartFulfill", delivery, e);
		}
	}

	public void cancelDeliveryOrder(String deliveryOrderId) throws DeliveryException {
		pidgeClient.cancelDeliveryOrder(deliveryOrderId).subscribe();
		Delivery delivery = findByDeliveryOrderId(deliveryOrderId);
		delivery.setDeleted(true);
		delivery.setStatus(DeliveryOrderStatusType.CANCELLED);
		save(delivery);
	}

	public DeliveryRiderLocation getDeliveryRiderLocation(String deliveryOrderId) throws DeliveryException {
		return pidgeClient.getDeliveryRiderLocation(deliveryOrderId);
	}

	public DeliveryOrderStatus getDeliveryOrderStatus(String deliveryOrderId) throws DeliveryException {
		return pidgeClient.getDeliveryOrderStatus(deliveryOrderId);
	}

	public void unallocateDeliveryOrder(String deliveryOrderId) throws DeliveryException {
		pidgeClient.unallocateDeliveryOrder(deliveryOrderId).subscribe();
		Delivery delivery = findByDeliveryOrderId(deliveryOrderId);
		delivery.setStatus(DeliveryOrderStatusType.PENDING);
		save(delivery);
	}

	private void handleDeliveryError(String action, Delivery delivery, Exception e) throws DeliveryException {
		orderService.updateOrderStatus(delivery.getOrderId(), OrderStatusType.DELIVERY_ERROR);
		log.error("Exception occurred in {} : {}", action, e.getMessage(), e);
		throw new DeliveryException(action, "Exception occurred in Delivery Service : " + e.getMessage(), e);
	}

	public void setFullfillExpiry(String orderId) {
		String fulfillRedisKey = "order:" + orderId + ":fulfill";
		redisService.setRedisData(fulfillRedisKey, OrderStatusType.ACCEPTED, Duration.ofMinutes(5).getSeconds());
		String deliveryRedisKey = "order:" + orderId + ":delivery";
		redisService.setRedisData(deliveryRedisKey, OrderStatusType.ACCEPTED, Duration.ofMinutes(6).getSeconds());
	}
}
