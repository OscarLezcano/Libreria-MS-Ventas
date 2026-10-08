package com.bigobooks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.repositories.CouponRepository;

/**
 * Verifica findDeleted()/findAllIncludingDeleted() de BaseRepository contra la
 * base de datos real. Requiere src/main/resources/config/private.properties;
 * se ejecuta solo si se define la propiedad de sistema sales.test.full=true.
 */
@SpringBootTest(classes = SalesApplication.class)
@EnabledIfSystemProperty(named = "sales.test.full", matches = "true")
class BaseRepositoryDeletionTests {

    @Autowired
    private CouponRepository coupons;

    @Autowired
    private DataSource dataSource;

    @Test
    void findDeletedAndFindAllIncludingDeletedReadSoftDeletedRows() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String code = "REPO-TEST-" + System.nanoTime();
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountPercent(5);
        coupon.setValidUntil(LocalDateTime.of(2026, 12, 31, 23, 59));
        coupons.saveAndFlush(coupon);
        try {
            assertThat(ids(coupons.findAll())).contains(coupon.getId());
            assertThat(ids(coupons.findAllIncludingDeleted())).contains(coupon.getId());
            assertThat(ids(coupons.findDeleted())).doesNotContain(coupon.getId());

            coupon.setDeleted(true);
            coupons.saveAndFlush(coupon);

            assertThat(ids(coupons.findAll())).doesNotContain(coupon.getId());
            assertThat(ids(coupons.findAllIncludingDeleted())).contains(coupon.getId());
            assertThat(ids(coupons.findDeleted())).contains(coupon.getId());
        } finally {
            jdbc.update("DELETE FROM coupon WHERE code = ?", code);
        }
    }

    private static List<Long> ids(List<Coupon> coupons) {
        return coupons.stream().map(Coupon::getId).toList();
    }
}
