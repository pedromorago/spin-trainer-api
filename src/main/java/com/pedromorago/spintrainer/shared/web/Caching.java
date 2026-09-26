package com.pedromorago.spintrainer.shared.web;

import org.springframework.http.CacheControl;

/** Políticas de caché HTTP de la API. */
public final class Caching {

    /**
     * Catálogo y rangos de referencia: solo cambian con una migración. El navegador puede guardarlos, pero revalida en
     * cada uso con el {@code ETag} ({@code 304} si no cambió). {@code private}: la respuesta va con el JWT del usuario.
     */
    public static final CacheControl REVALIDATE = CacheControl.noCache().cachePrivate();

    private Caching() {}
}
