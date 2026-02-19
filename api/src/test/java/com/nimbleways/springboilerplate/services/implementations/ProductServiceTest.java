package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldNotifyDelayAndSaveProduct() {
        Product product = new Product();
        product.setName("TV");
        product.setLeadTime(5);

        productService.notifyDelay(10, product);

        assertEquals(10, product.getLeadTime());
        verify(productRepository).save(product);
        verify(notificationService).sendDelayNotification(10, "TV");
    }

    @Test
    void shouldSendOutOfStockIfLeadTimeExceedsSeasonEnd() {
        Product product = new Product();
        product.setName("Ice Cream");
        product.setLeadTime(10);
        product.setAvailable(5);
        product.setSeasonStartDate(LocalDate.now().minusDays(5));
        product.setSeasonEndDate(LocalDate.now().plusDays(5));

        productService.handleSeasonalProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendOutOfStockNotification("Ice Cream");
        verify(productRepository).save(product);
    }

    @Test
    void shouldSendOutOfStockIfSeasonNotStartedYet() {
        Product product = new Product();
        product.setName("Winter Jacket");
        product.setLeadTime(5);
        product.setAvailable(3);
        product.setSeasonStartDate(LocalDate.now().plusDays(5));
        product.setSeasonEndDate(LocalDate.now().plusDays(20));

        productService.handleSeasonalProduct(product);

        verify(notificationService).sendOutOfStockNotification("Winter Jacket");
        verify(productRepository).save(product);
    }



    @Test
    void shouldDecreaseStockIfNotExpired() {
        Product product = new Product();
        product.setName("Milk");
        product.setAvailable(5);
        product.setExpiryDate(LocalDate.now().plusDays(5));

        productService.handleExpiredProduct(product);

        assertEquals(4, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never())
                .sendExpirationNotification(any(), any());
    }

    @Test
    void shouldSendExpirationNotificationIfExpired() {
        Product product = new Product();
        product.setName("Yogurt");
        product.setAvailable(5);
        product.setExpiryDate(LocalDate.now().minusDays(1));

        productService.handleExpiredProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService)
                .sendExpirationNotification("Yogurt", product.getExpiryDate());
        verify(productRepository).save(product);
    }

    @Test
    void processProduct_shouldDecreaseStockAndSave_whenNormalProductInStock() {
        Product p = new Product();
        p.setType("NORMAL");
        p.setAvailable(5);

        productService.processProduct(p);

        Mockito.verify(productRepository).save(p);
        assertThat(p.getAvailable()).isEqualTo(4);
    }

    @Test
    void processProduct_shouldCallNotifyDelay_whenNormalProductOutOfStockWithLeadTime() {
        Product p = new Product();
        p.setType("NORMAL");
        p.setAvailable(0);
        p.setLeadTime(3);

        productService.processProduct(p);

        Mockito.verify(notificationService).sendDelayNotification(3, p.getName());
    }

    @Test
    void processProduct_shouldDoNothing_whenNormalProductOutOfStockAndNoLeadTime() {
        Product p = new Product();
        p.setType("NORMAL");
        p.setAvailable(0);
        p.setLeadTime(0);

        productService.processProduct(p);

        Mockito.verifyNoInteractions(productRepository, notificationService);
    }

    @Test
    void processProduct_shouldDecreaseStock_whenSeasonalProductInSeasonAndInStock() {
        Product p = new Product();
        p.setType("SEASONAL");
        p.setAvailable(5);
        p.setLeadTime(0);
        p.setSeasonStartDate(LocalDate.now().minusDays(1));
        p.setSeasonEndDate(LocalDate.now().plusDays(1));

        productService.processProduct(p);

        Mockito.verify(productRepository).save(p);
        assertThat(p.getAvailable()).isEqualTo(4);
    }

    @Test
    void processProduct_shouldCallHandleSeasonal_whenSeasonalProductOutOfSeason() {
        Product p = new Product();
        p.setType("SEASONAL");
        p.setAvailable(5);
        p.setSeasonStartDate(LocalDate.now().plusDays(10));
        p.setSeasonEndDate(LocalDate.now().plusDays(20));
        p.setLeadTime(1);

        productService.processProduct(p);

        Mockito.verify(notificationService).sendOutOfStockNotification(p.getName());
    }

    @Test
    void processProduct_shouldDecreaseStock_whenExpirableProductInStockAndNotExpired() {
        Product p = new Product();
        p.setType("EXPIRABLE");
        p.setAvailable(5);
        p.setExpiryDate(LocalDate.now().plusDays(10));

        productService.processProduct(p);

        Mockito.verify(productRepository).save(p);
        assertThat(p.getAvailable()).isEqualTo(4);
    }

    @Test
    void processProduct_shouldSendExpirationNotification_whenExpirableProductIsExpired() {
        Product p = new Product();
        p.setType("EXPIRABLE");
        p.setAvailable(5);
        p.setExpiryDate(LocalDate.now().minusDays(1));

        productService.processProduct(p);

        Mockito.verify(notificationService).sendExpirationNotification(p.getName(), p.getExpiryDate());
    }
}

