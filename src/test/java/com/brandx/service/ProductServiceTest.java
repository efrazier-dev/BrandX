package com.brandx.service;

import com.brandx.domain.Product;
import com.brandx.repository.OrderItemRepository;
import com.brandx.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private OrderItemRepository orderItems;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, orderItems);
    }

    @Test
    void list_blankSearch_delegatesToFindAll() {
        Pageable pageable = Pageable.unpaged();
        Page<Product> page = new PageImpl<>(List.of(aProduct(1L, "SKU-1", "Widget")));
        when(products.findAll(pageable)).thenReturn(page);

        Page<Product> result = service.list("  ", pageable);

        assertThat(result).isSameAs(page);
        verify(products, never()).findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void list_withSearchTerm_delegatesToNameOrSkuSearch() {
        Pageable pageable = Pageable.unpaged();
        Page<Product> page = new PageImpl<>(List.of(aProduct(1L, "SKU-1", "Widget")));
        when(products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase("widget", "widget", pageable))
                .thenReturn(page);

        Page<Product> result = service.list("widget", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void get_found_returnsProduct() {
        Product product = aProduct(1L, "SKU-1", "Widget");
        when(products.findById(1L)).thenReturn(Optional.of(product));

        assertThat(service.get(1L)).isSameAs(product);
    }

    @Test
    void get_notFound_throwsNotFoundException() {
        when(products.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void activeProducts_delegatesToFindByActiveTrueOrderByNameAsc() {
        List<Product> active = List.of(aProduct(1L, "SKU-1", "Widget"));
        when(products.findByActiveTrueOrderByNameAsc()).thenReturn(active);

        assertThat(service.activeProducts()).isSameAs(active);
    }

    @Test
    void count_delegatesToRepositoryCount() {
        when(products.count()).thenReturn(7L);

        assertThat(service.count()).isEqualTo(7L);
    }

    @Test
    void create_success_savesFreshEntityNotSubmittedInstance() {
        Product submitted = aProduct(999L, "SKU-NEW", "Widget");
        submitted.setDescription("desc");
        submitted.setPrice(new BigDecimal("19.99"));
        submitted.setStockQuantity(5);
        submitted.setActive(false);
        when(products.existsBySkuIgnoreCase("SKU-NEW")).thenReturn(false);
        when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.create(submitted);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(products).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved).isNotSameAs(submitted);
        assertThat(saved.getId()).isNull();
        assertThat(saved.getSku()).isEqualTo("SKU-NEW");
        assertThat(saved.getName()).isEqualTo("Widget");
        assertThat(saved.getDescription()).isEqualTo("desc");
        assertThat(saved.getPrice()).isEqualByComparingTo("19.99");
        assertThat(saved.getStockQuantity()).isEqualTo(5);
        assertThat(saved.isActive()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_duplicateSku_throwsDuplicateValueException_andNeverSaves() {
        Product submitted = aProduct(null, "SKU-DUP", "Widget");
        when(products.existsBySkuIgnoreCase("SKU-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.create(submitted))
                .isInstanceOf(DuplicateValueException.class)
                .satisfies(ex -> assertThat(((DuplicateValueException) ex).getField()).isEqualTo("sku"));
        verify(products, never()).save(any());
    }

    @Test
    void create_blankSku_skipsUniquenessCheck() {
        Product submitted = aProduct(null, "  ", "Widget");
        when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(submitted);

        verify(products, never()).existsBySkuIgnoreCase(any());
    }

    @Test
    void update_success_mutatesManagedEntityInPlace() {
        Product existing = aProduct(1L, "SKU-OLD", "Old Name");
        Product submitted = aProduct(null, "SKU-NEW", "New Name");
        submitted.setDescription("new desc");
        submitted.setPrice(new BigDecimal("29.99"));
        submitted.setStockQuantity(10);
        submitted.setActive(false);
        when(products.findById(1L)).thenReturn(Optional.of(existing));
        when(products.existsBySkuIgnoreCaseAndIdNot("SKU-NEW", 1L)).thenReturn(false);
        when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.update(1L, submitted);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getSku()).isEqualTo("SKU-NEW");
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("new desc");
        assertThat(existing.getPrice()).isEqualByComparingTo("29.99");
        assertThat(existing.getStockQuantity()).isEqualTo(10);
        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void update_duplicateSkuExcludingSelf_throwsDuplicateValueException() {
        Product existing = aProduct(1L, "SKU-OLD", "Old Name");
        Product submitted = aProduct(null, "SKU-TAKEN", "New Name");
        when(products.findById(1L)).thenReturn(Optional.of(existing));
        when(products.existsBySkuIgnoreCaseAndIdNot("SKU-TAKEN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, submitted)).isInstanceOf(DuplicateValueException.class);
        verify(products, never()).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException_beforeUniquenessCheck() {
        when(products.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, aProduct(null, "SKU-1", "Widget")))
                .isInstanceOf(NotFoundException.class);
        verify(products, never()).existsBySkuIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void delete_success_deletesWhenNoOrderLines() {
        Product product = aProduct(1L, "SKU-1", "Widget");
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(orderItems.countByProductId(1L)).thenReturn(0L);

        service.delete(1L);

        verify(products).delete(product);
    }

    @Test
    void delete_throwsEntityInUseException_whenReferencedByOrderLines() {
        Product product = aProduct(1L, "SKU-1", "Widget");
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(orderItems.countByProductId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(EntityInUseException.class);
        verify(products, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFoundException_beforeCountCheck() {
        when(products.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
        verify(orderItems, never()).countByProductId(any());
    }

    private static Product aProduct(Long id, String sku, String name) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setPrice(BigDecimal.TEN);
        product.setStockQuantity(0);
        product.setActive(true);
        return product;
    }
}
