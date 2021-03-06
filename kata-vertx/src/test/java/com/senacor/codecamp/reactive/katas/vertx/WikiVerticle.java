package com.senacor.codecamp.reactive.katas.vertx;

import com.senacor.codecamp.reactive.services.WikiService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author Andreas Keefer
 */
public class WikiVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(WikiVerticle.class);

    private final WikiService wikiService = WikiService.create();

    @Override
    public void start() throws Exception {
        vertx.eventBus().<String>consumer("fetchArticle").handler(msg ->
                wikiService.fetchArticleObservable(msg.body())
                        .subscribe(msg::reply)
        );
    }
}
