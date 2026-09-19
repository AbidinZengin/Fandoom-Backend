package com.example.fandoom_backend.common.specification;

import org.springframework.data.jpa.domain.Specification;

// "(sortField, id) DESC" sıralamasında verilen işaretçiden SONRAKİ satırlar:
//   sortField < :value  OR  (sortField = :value AND id < :id)
// OFFSET'in aksine sayfa derinliğinden bağımsız, indeks üzerinden doğrudan konumlanır.
public final class KeysetSpecification {

    private KeysetSpecification() {}

    public static <T, V extends Comparable<? super V>> Specification<T> after(
            String sortField, V value, Long id) {
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.<V>get(sortField), value),
                cb.and(cb.equal(root.get(sortField), value), cb.lessThan(root.<Long>get("id"), id)));
    }
}
