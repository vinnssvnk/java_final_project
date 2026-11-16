package com.ivanna.evcharging.infrastructure.io;

import java.util.List;

/** Outcome of reading one CSV file: the rows that could be mapped and a message per skipped row. */
public record LoadResult<T>(List<T> items, List<String> errors) {

    public LoadResult {
        items = List.copyOf(items);
        errors = List.copyOf(errors);
    }
}
