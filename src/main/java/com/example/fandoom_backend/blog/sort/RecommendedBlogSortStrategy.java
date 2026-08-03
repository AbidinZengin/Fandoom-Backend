package com.example.fandoom_backend.blog.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class RecommendedBlogSortStrategy implements BlogSortStrategy {

    @Override
    public String key() {
        return "recommended";
    }

    @Override
    public Sort toJpaSort() {
        // Sıra JPQL/Hibernate seviyesinde standart Sort.Order ile ifade
        // edilemiyor: recommendedRank null olan (editörün hiç sıralamadığı)
        // blog'ların her zaman EN SONA düşmesi gerekiyor, ancak varsayılan
        // ASC/DESC null handling (NullHandling.NATIVE) veritabanına göre
        // değişken davranır (MySQL'de NULL'lar ASC'de en küçük kabul edilir,
        // yani en başa düşer — istediğimizin tam tersi). Bu yüzden JPA'nın
        // Sort.Order#nullsLast() bayrağını (Hibernate 6 tarafından
        // "ORDER BY ... NULLS LAST" olarak SQL standardına uygun şekilde
        // çevrilir) kullanıyoruz; bu, ham JpaSort.unsafe(...) ile veritabanına
        // özgü SQL yazmaktan daha idiyomatik ve MySQL dışı sürücülere de taşınabilir.
        return Sort.by(Sort.Order.asc("recommendedRank").nullsLast());
    }
}
