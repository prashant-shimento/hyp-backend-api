package com.hyp.translation;

import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.request.DeliveryFulfillRequest;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryOrderRequest.ContactDetail;
import com.hyp.request.DeliveryOrderRequest.Trip;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.DeliveryQuoteRequest.Coordinates;
import com.hyp.request.DeliveryQuoteRequest.Drop;
import com.hyp.request.DeliveryQuoteRequest.Location;
import com.hyp.request.DeliveryQuoteRequest.Pickup;
import com.hyp.util.CommonUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRequestTranslation {

    private static String platformSupportContact;
    private static String restaurantSupportContact;

    @Value("${platform.support.contact}")
    public void setPlatformSupportContact(String value) {
        platformSupportContact = value;
    }

    @Value("${restaurant.support.contact}")
    public void setRestaurantSupportContact(String value) {
        restaurantSupportContact = value;
    }

    public static DeliveryFulfillRequest getOrderFulfillRequest(Delivery delivery) {
        List<String> orderIds = new ArrayList<>();
        orderIds.add(delivery.getDeliveryOrderId());
        return DeliveryFulfillRequest.builder()
                .ids(orderIds)
                .service(delivery.getService())
                .pickUpNow(true)
                .networkId(delivery.getNetworkId())
                .token(delivery.getNetworkToken())
                .build();
    }

    public static DeliveryFulfillRequest getSmartFulfillRequest(Delivery delivery) {
        List<String> orderIds = new ArrayList<>();
        orderIds.add(delivery.getDeliveryOrderId());
        return DeliveryFulfillRequest.builder().ids(orderIds).build();
    }

    public static DeliveryQuoteRequest getQuoteRequest(Restaurant restaurant, Address address) {
        DeliveryQuoteRequest quoteRequest = new DeliveryQuoteRequest();

        Pickup pickup = new Pickup();
        Coordinates pickupCoordinates = new Coordinates();
        pickupCoordinates.setLatitude(restaurant.getLocation().getLatitude());
        pickupCoordinates.setLongitude(restaurant.getLocation().getLongitude());
        pickup.setCoordinates(pickupCoordinates);
        pickup.setPincode(restaurant.getPincode());
        quoteRequest.setPickup(pickup);

        List<Drop> dropList = new ArrayList<>();
        Drop drop = new Drop();
        drop.setRef(CommonUtils.generateReferenceId("PGQ"));
        Location dropLocation = new Location();
        Coordinates dropCoordinates = new Coordinates();
        dropCoordinates.setLatitude(address.getLocation().getLatitude());
        dropCoordinates.setLongitude(address.getLocation().getLongitude());
        dropLocation.setCoordinates(dropCoordinates);
        dropLocation.setPincode(address.getPincode());
        drop.setLocation(dropLocation);
        dropList.add(drop);
        quoteRequest.setDrop(dropList);
        return quoteRequest;
    }

    public static DeliveryOrderRequest getDeliveryOrderRequest(
            Restaurant restaurant, Address address, Customer customer, Order order) {
        DeliveryOrderRequest orderRequest = new DeliveryOrderRequest();

        ContactDetail senderDetail = new ContactDetail();
        DeliveryOrderRequest.Address senderAddress = new DeliveryOrderRequest.Address();
        senderAddress.setAddressLine1(restaurant.getAddress());
        senderAddress.setPincode(restaurant.getPincode());
        senderAddress.setCity(restaurant.getCity());
        senderAddress.setState(restaurant.getState());
        senderAddress.setCountry(restaurant.getCountry());
        senderAddress.setLatitude(restaurant.getLocation().getLatitude());
        senderAddress.setLongitude(restaurant.getLocation().getLongitude());
        senderDetail.setAddress(senderAddress);
        senderDetail.setMobile(restaurantSupportContact);
        senderDetail.setName(restaurant.getRestaurantName());
        orderRequest.setSenderDetail(senderDetail);

        ContactDetail pocDetail = new ContactDetail();
        pocDetail.setName("Hyperapps");
        pocDetail.setMobile(platformSupportContact);
        orderRequest.setPocDetail(pocDetail);

        ContactDetail receiverDetail = new ContactDetail();
        DeliveryOrderRequest.Address receiverAddress = new DeliveryOrderRequest.Address();
        receiverAddress.setAddressLine1(address.getAddressOne());
        receiverAddress.setAddressLine2(address.getAddressTwo());
        receiverAddress.setPincode(address.getPincode());
        receiverAddress.setCity(address.getCity());
        receiverAddress.setState(address.getState());
        receiverAddress.setCountry(address.getCountry());
        receiverAddress.setLatitude(address.getLocation().getLatitude());
        receiverAddress.setLongitude(address.getLocation().getLongitude());
        receiverDetail.setAddress(receiverAddress);
        receiverDetail.setMobile(customer.getMobile());
        receiverDetail.setName(customer.getName());
        List<Trip> trips = new ArrayList<>();
        Trip trip = new Trip();
        trip.setReferenceId(order.getId());
        trip.setReceiverDetail(receiverDetail);
        trip.setSourceOrderId(order.getId());
        trip.setBillAmount(order.getTotalAmount());
        trips.add(trip);
        orderRequest.setTrips(trips);
        return orderRequest;
    }

    public static Delivery getDeliveryEntity(DeliveryOrderRequest deliveryOrderRequest) {
        Delivery delivery = new Delivery();
        delivery.setChannel(deliveryOrderRequest.getChannel());

        Delivery.ContactDetail senderDetail = new Delivery.ContactDetail();
        Delivery.Address senderAddress = new Delivery.Address();
        BeanUtils.copyProperties(deliveryOrderRequest.getSenderDetail(), senderDetail);
        BeanUtils.copyProperties(deliveryOrderRequest.getSenderDetail().getAddress(), senderAddress);
        senderDetail.setAddress(senderAddress);
        delivery.setSenderDetail(senderDetail);

        Delivery.ContactDetail pocDetail = new Delivery.ContactDetail();
        BeanUtils.copyProperties(deliveryOrderRequest.getPocDetail(), pocDetail);
        delivery.setPocDetail(pocDetail);

        Trip trip = deliveryOrderRequest.getTrips().get(0);
        delivery.setAmount(trip.getBillAmount());
        delivery.setReferenceId(trip.getReferenceId());
        delivery.setOrderId(trip.getSourceOrderId());

        Delivery.ContactDetail receiverDetail = new Delivery.ContactDetail();
        Delivery.Address receiverAddress = new Delivery.Address();
        BeanUtils.copyProperties(trip.getReceiverDetail(), receiverDetail);
        BeanUtils.copyProperties(trip.getReceiverDetail().getAddress(), receiverAddress);
        receiverDetail.setAddress(receiverAddress);
        delivery.setReceiverDetail(receiverDetail);

        return delivery;
    }
}
