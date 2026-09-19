package com.example.fandoom_backend.blog.service;

// Blog cache adları (RedisConfig'te tek global TTL).
//  DETAIL: slug ile public detay   LIST: public liste + "ilgili içerikler" merdiveni
//  HUB:    hub filtre sonuçları + facet sayaçları (tag/ ve franchise/ yazmalarından da temizlenir)
public final class BlogCacheNames {

    public static final String DETAIL = "blog:detail";
    public static final String LIST = "blog:list";
    public static final String HUB = "blog:hub";

    private BlogCacheNames() {
    }
}
