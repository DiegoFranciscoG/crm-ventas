package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @EntityGraph(attributePaths = {"vatRate"})
    List<Product> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"vatRate"})
    List<Product> findByActiveTrueOrderByNameAsc();
}
