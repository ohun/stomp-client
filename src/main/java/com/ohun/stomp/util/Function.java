package com.ohun.stomp.util;

/**
 * Created by xiaoxu.yxx on 2014/9/30.
 */
public interface Function<I, O> {

    O apply(I input);
}
