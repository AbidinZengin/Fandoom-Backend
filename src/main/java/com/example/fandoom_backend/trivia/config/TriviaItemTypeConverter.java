package com.example.fandoom_backend.trivia.config;

import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// ?itemType=movie (küçük harf) query parametresi için; Spring'in varsayılan String→enum
// dönüşümü büyük/küçük harfe duyarlıdır.
@Component
public class TriviaItemTypeConverter implements Converter<String, TriviaItemType> {

    @Override
    public TriviaItemType convert(String source) {
        return TriviaItemType.fromValue(source);
    }
}
