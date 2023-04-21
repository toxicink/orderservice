package edu.iu.c322.demo.orderservice.controller;

import edu.iu.c322.demo.orderservice.model.Address;
import edu.iu.c322.demo.orderservice.model.Order;
import edu.iu.c322.demo.orderservice.model.OrderItem;
import edu.iu.c322.demo.orderservice.model.Return;
import edu.iu.c322.demo.orderservice.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Order order) {
        // Check that customerId is present
        Integer customerId = order.getCustomerId();
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing customerId");
        }


        // Check that at least one item is present
        if (order.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("At least one item is required");
        }

        // Check that each item has a name, quantity, and price
        for (OrderItem item : order.getItems()) {
            if (item.getName() == null || item.getQuantity() == 0 || item.getPrice() == 0.0) {
                return ResponseEntity.badRequest().body("All items must have a name, quantity, and price");
            }
        }

        // Check that payment method and billing address are present
        if (order.getPayment() == null || order.getPayment().getMethod() == null || order.getPayment().getBillingAddress() == null) {
            return ResponseEntity.badRequest().body("Payment method and billing address are required");
        }

        // Check that billing address has a state, city, and postal code
        Address billingAddress = order.getPayment().getBillingAddress();
        if (billingAddress.getState() == null || billingAddress.getCity() == null || billingAddress.getPostalCode() == 0) {
            return ResponseEntity.badRequest().body("Billing address must have a state, city, and postal code");
        }

        // Check that shipping address has a state, city, and postal code
        if (order.getShippingAddress() != null) {
            Address shippingAddress = order.getShippingAddress();
            if (shippingAddress.getState() == null || shippingAddress.getCity() == null || shippingAddress.getPostalCode() == 0) {
                return ResponseEntity.badRequest().body("Shipping address must have a state, city, and postal code");
            }
        }

        // Set the order on each item
        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
        }

        // Save the order to the database
        Order addedOrder = repository.save(order);

        // Return the ID of the newly created order
        return ResponseEntity.status(HttpStatus.CREATED).body(addedOrder.getId());
    }


    @GetMapping("/{customerId}")
    public List<Order> findByCustomer(@PathVariable int customerId){
        return repository.findByCustomerId(customerId);
    }

    @GetMapping("/order/{orderId}")
    public Optional<Order> findByOrderId(@PathVariable int orderId){
        return repository.findById(orderId);
    }

    @DeleteMapping("/{orderId}")
    public void deleteOrder(@PathVariable int orderId) {
        Optional<Order> order = repository.findById(orderId);
        if (order.isPresent()) {
            repository.delete(order.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }
    @PutMapping("/return")
    public ResponseEntity<String> submitReturnRequest(@RequestBody Return request) {
        int orderId = request.getOrderId();
        int itemId = request.getItemId();
        String reason = request.getReason();

        Optional<Order> optionalOrder = repository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }

        Order order = optionalOrder.get();
        boolean itemFound = false;
        for (OrderItem item : order.getItems()) {
            if (item.getId() == itemId) {
                itemFound = true;
                item.setReturnRequested(true);
                item.setReturnReason(reason);
                repository.save(order);
                break;
            }
        }

        if (itemFound) {
            return ResponseEntity.ok("Return request submitted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found in order");
        }
    }

}
