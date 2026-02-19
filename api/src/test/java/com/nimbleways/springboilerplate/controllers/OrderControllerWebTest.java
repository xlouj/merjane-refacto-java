package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.services.implementations.ProductService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    void processOrder_shouldReturn200AndOrderId() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setItems(Set.of(new Product()));

        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/orders/1/processOrder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void processOrder_shouldCallProcessProductForEachItem() throws Exception {
        Product p1 = new Product();
        Product p2 = new Product();

        Order order = new Order();
        order.setId(2L);
        order.setItems(Set.of(p1, p2));

        Mockito.when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/orders/2/processOrder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        // Controller's only responsibility: delegate each product to the service
        Mockito.verify(productService).processProduct(p1);
        Mockito.verify(productService).processProduct(p2);
    }

    @Test
    void processOrder_shouldReturn404_whenOrderNotFound() throws Exception {
        Mockito.when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/orders/99/processOrder"))
                .andExpect(status().isNotFound());
    }
}