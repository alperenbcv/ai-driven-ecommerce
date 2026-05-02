package com.ecommerce.common.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Tüm servis sınıflarının implemente edebileceği jenerik bir servis interface'idir.
 */
public interface CrudService<T, ID> {

    T findById(ID id);

    Page<T> findAll(Pageable pageable);

    T save(T dto);

    T update(ID id, T dto);

    void deleteById(ID id);
}
