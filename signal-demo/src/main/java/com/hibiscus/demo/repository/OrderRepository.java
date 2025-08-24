package com.hibiscus.demo.repository;

import com.hibiscus.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 订单仓库
 * 
 * @author heathcetide
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据订单号查找订单
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 检查订单号是否存在
     */
    boolean existsByOrderNo(String orderNo);
}
