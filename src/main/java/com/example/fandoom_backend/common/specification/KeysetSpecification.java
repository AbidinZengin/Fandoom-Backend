package com.example.fandoom_backend.common.specification;

import org.springframework.data.jpa.domain.Specification;

// "ORDER BY sortField DESC, id ASC" sıralamasında verilen işaretçiden SONRAKİ satırlar:
//   sortField < :value  OR  (sortField = :value AND id > :id)
// OFFSET'in aksine sayfa derinliğinden bağımsız, indeks üzerinden doğrudan konumlanır.
// NEDEN id ASC (DESC değil): InnoDB ikincil indeksi (status, x DESC) örtük olarak PK'yı id ASC ekler;
// ORDER BY'ın id yönü bununla ters olursa (x DESC, id DESC) MySQL indeksi sıralama için kullanamaz ve
// FILESORT yapar (EXPLAIN ile doğrulandı). id ASC ile indeks ileri taranır, filesort yok.
public final class KeysetSpecification {

    private KeysetSpecification() {}

    public static <T, V extends Comparable<? super V>> Specification<T> after(
            String sortField, V value, Long id) {
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.<V>get(sortField), value),
                cb.and(cb.equal(root.get(sortField), value), cb.greaterThan(root.<Long>get("id"), id)));
    }
}
