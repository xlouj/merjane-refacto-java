package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

import javax.transaction.Transactional;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ProductService(ProductRepository productRepository,
                          NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void processProduct(Product p) {
        switch (p.getType()) {
            case "NORMAL"    -> handleNormalProduct(p);
            case "SEASONAL"  -> handleSeasonalProduct(p);
            case "EXPIRABLE" -> handleExpiredProduct(p);
            default          -> log.warn("Unknown product type: {}", p.getType());
        }
    }

    private void handleNormalProduct(Product p) {
        if (p.getAvailable() > 0) {
            p.setAvailable(p.getAvailable() - 1);
            productRepository.save(p);
        } else if (p.getLeadTime() > 0) {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    public void handleSeasonalProduct(Product p) {
        LocalDate today = LocalDate.now();
        if (today.plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(p.getName());
            p.setAvailable(0);
            productRepository.save(p);
        } else if (p.getSeasonStartDate().isAfter(today)) {
            notificationService.sendOutOfStockNotification(p.getName());
            productRepository.save(p);
        } else if (today.isAfter(p.getSeasonStartDate())
                && today.isBefore(p.getSeasonEndDate())
                && p.getAvailable() > 0) {
            p.setAvailable(p.getAvailable() - 1);
            productRepository.save(p);
        } else {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    public void handleExpiredProduct(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - 1);
            productRepository.save(p);
        } else {
            notificationService.sendExpirationNotification(p.getName(), p.getExpiryDate());
            p.setAvailable(0);
            productRepository.save(p);
        }
    }

    public void notifyDelay(int leadTime, Product p) {
        p.setLeadTime(leadTime);
        productRepository.save(p);
        notificationService.sendDelayNotification(leadTime, p.getName());
    }
}