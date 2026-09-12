package com.example.fandoom_backend.community.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// account/UserLike ile aynı desen (surrogate id + unique constraint, composite
// PK değil) — ama bilinçli olarak BAĞIMSIZ: account/'daki genel SavedItemType
// tabanlı Like mekanizması Community için kasıtlı olarak kullanılmıyor.
@Entity
@Table(name = "thread_like", uniqueConstraints = {
        @UniqueConstraint(name = "uk_thread_like", columnNames = {"user_id", "thread_id"})
}, indexes = {
        @Index(name = "idx_thread_like_thread", columnList = "thread_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ThreadLike extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;
}
