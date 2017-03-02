package com.senacor.tecco.reactive.katas.vertx;

import com.senacor.tecco.reactive.ReactiveUtil;
import com.senacor.tecco.reactive.vertx.RxVertx;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import io.vertx.core.DeploymentOptions;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;

/**
 * @author Andreas Keefer
 */
public class WikiVerticleRxTest extends VertxTestBase {

    public static final String FETCH_ARTICLE = "fetchArticle";

    @Test
    public void testfetchArticle() throws Exception {
        vertx.deployVerticle(WikiVerticle.class.getName(), new DeploymentOptions().setWorker(true));
        waitUntil(() -> vertx.deploymentIDs().size() == 1);
        System.out.println("deployed verticles:" + vertx.deploymentIDs());

        vertx.eventBus().registerDefaultCodec(ParsedPage.class, new ParsedPageCodec());

        RxVertx.<String>send(vertx, FETCH_ARTICLE, "42").subscribe(
                s -> {
                    ReactiveUtil.print(s);
                    assertThat(s, containsString("42"));
                    testComplete();
                },
                error -> {
                    error.printStackTrace();
                    fail("failed: " + error);
                }
        );

        await(5, TimeUnit.SECONDS);
    }

}
