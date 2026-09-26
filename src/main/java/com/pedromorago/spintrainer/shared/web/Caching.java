package com.pedromorago.spintrainer.shared.web;

import org.springframework.http.CacheControl;

/** HTTP cache policies of the API. */
public final class Caching {

    /**
     * Catalog and reference ranges only change with a migration. The browser may store them but revalidates on each use
     * with the {@code ETag} ({@code 304} if unchanged). {@code private}: the response is tied to the user's JWT.
     */
    public static final CacheControl REVALIDATE = CacheControl.noCache().cachePrivate();

    private Caching() {}
}
