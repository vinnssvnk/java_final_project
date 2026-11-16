package com.ivanna.evcharging.infrastructure.persistence;

import com.ivanna.evcharging.domain.repository.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Generic map-backed repository; the id extractor lets one class serve every entity type. */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {

    // LinkedHashMap keeps listings in the order the data was loaded.
    private final Map<ID, T> entities = new LinkedHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryRepository(Function<T, ID> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor must not be null");
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(entities.values());
    }

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        entities.put(idExtractor.apply(entity), entity);
        return entity;
    }
}
