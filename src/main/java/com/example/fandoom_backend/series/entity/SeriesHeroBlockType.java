package com.example.fandoom_backend.series.entity;

// TITLE/META/SYNOPSIS/BUTTON kendi içerik alanını taşımaz — render anında
// Series'in kendi alanlarından (titleTr/title, synopsisTr/synopsis, trailerUrl)
// otomatik beslenir, burada yalnızca konum+stil tutulur. IMAGE/LOGO
// imageUrl taşır, BOX serbest textTr/text+backgroundColor taşır.
public enum SeriesHeroBlockType {
    IMAGE, LOGO, TITLE, META, SYNOPSIS, BUTTON, BOX
}
