package com.example.fandoom_backend.media.service;

import com.example.fandoom_backend.media.dto.VideoUploadSignatureResponse;

// Sunucuya bayt taşımadan, istemcinin doğrudan storage sağlayıcısına video yüklemesi için imzalı parametreler üretir.
public interface VideoUploadSigner {

    VideoUploadSignatureResponse createSignature();
}
