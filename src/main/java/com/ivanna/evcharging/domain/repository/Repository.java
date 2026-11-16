package com.ivanna.evcharging.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction owned by the domain. Services depend on this interface, and the
 * infrastructure layer supplies an implementation (dependency inversion).
 */
public interface Repository<T, ID> {

    Optional<T> findById(ID id);

    /** All entities as an unmodifiable list. */
    List<T> findAll();

    /** Inserts the entity or replaces the one with the same id. */
    T save(T entity);
}
