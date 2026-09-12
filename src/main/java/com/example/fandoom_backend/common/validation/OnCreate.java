package com.example.fandoom_backend.common.validation;

// Bean Validation grup işaretleyicisi: aynı request record'unun POST (tam
// doğrulama, bkz. OnCreate) ile PATCH (kısmi güncelleme, null="değişmedi",
// bkz. OnUpdate) uçlarında farklı zorunluluk kurallarıyla doğrulanması
// gerektiğinde kullanılır — controller'da @Validated(OnCreate.class) /
// @Validated(OnUpdate.class) ile seçilir (bkz. AccountController).
public interface OnCreate {
}
