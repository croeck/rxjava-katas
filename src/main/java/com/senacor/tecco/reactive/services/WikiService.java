package com.senacor.tecco.reactive.services;

import com.senacor.tecco.reactive.ReactiveUtil;
import com.senacor.tecco.reactive.services.integration.MediaWikiTextParser;
import com.senacor.tecco.reactive.services.integration.WikipediaServiceJapi;
import com.senacor.tecco.reactive.services.integration.WikipediaServiceJapiImpl;
import com.senacor.tecco.reactive.services.integration.WikipediaServiceJapiMock;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.senacor.tecco.reactive.ReactiveUtil.getThreadId;
import static com.senacor.tecco.reactive.ReactiveUtil.print;

/**
 * @author Andreas Keefer
 */
public class WikiService {

    private final MediaWikiTextParser parser = new MediaWikiTextParser();
    private final boolean MOCKMODE = true;
    private final WikipediaServiceJapi wikiServiceJapi;

    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    public WikiService() {
        if(MOCKMODE){
            wikiServiceJapi = new WikipediaServiceJapiMock();
        } else {
            wikiServiceJapi = new WikipediaServiceJapiImpl();
        }
    }

    public WikiService(String language) {
        if(MOCKMODE){
            wikiServiceJapi = new WikipediaServiceJapiMock();
        } else {
            wikiServiceJapi = new WikipediaServiceJapiImpl("http://" + language + ".wikipedia.org");
        }
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public Observable<String> fetchArticleObservable(final String wikiArticle) {
        return Observable.create(subscriber ->
            fetchArticleCompletableFuture(wikiArticle).whenComplete((result, error) -> {
                if (error != null) {
                    subscriber.onError(error);
                } else {
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                }
            }));
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public String fetchArticle(final String wikiArticle) {
        return wikiServiceJapi.getArticle(wikiArticle);
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public void fetchArticleCallback(final String wikiArticle, Consumer<String> articleConsumer, Consumer<Exception> exceptionConsumer) {
        try {
            fetchArticleCompletableFuture(wikiArticle)
                    .thenAccept(articleConsumer);
        } catch (Exception e) {
            exceptionConsumer.accept(e);
        }
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public Future<String> fetchArticleFuture(final String wikiArticle) {
        return pool.submit(() -> wikiServiceJapi.getArticle(wikiArticle));
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public CompletableFuture<String> fetchArticleCompletableFuture(final String wikiArticle) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> wikiServiceJapi.getArticle(wikiArticle), pool);
    }

    //---------------------------------------------------------------------------------

    /**
     * @param mediaWikiText Media Wiki formated text
     * @return parsed text in structured form
     */
    public Observable<ParsedPage> parseMediaWikiTextObservable(String mediaWikiText) {
        return parser.parseObservable(mediaWikiText);
    }


    /**
     * @param mediaWikiText Media Wiki formated text
     * @return parsed text in structured form
     */
    public ParsedPage parseMediaWikiText(String mediaWikiText) {
        return parser.parse(mediaWikiText);
    }


    public Future<ParsedPage> parseMediaWikiTextFuture(String mediaWikiText) {
        return pool.submit(() -> parseMediaWikiText(mediaWikiText));
    }


    public CompletableFuture<ParsedPage> parseMediaWikiTextCompletableFuture(String mediaWikiText) {
        return CompletableFuture.supplyAsync(() -> parseMediaWikiText(mediaWikiText), pool);
    }

    private static final List<String> WIKI_ARTICLES = Arrays.asList(
            "42", "Foo", "Korea", "Gaya", "Ostasien", "China", "Russland", "Gelbes_Meer",
            "Japanisches_Meer", "Observable", "Energie", "Zeitentwicklung", "Quantenmechanik",
            "Meter", "Kilogramm", "Lichtgeschwindigkeit", "Spin", "Wellenmechanik", "Erwin_Schrödinger",
            "Infinitesimalrechnung", "Joule", "Elektronenvolt", "Photon", "Teilchen", "Teilchen",
            "Astroide", "Pseudonorm", "Kompakter_Raum", "Isometrie", "Potenzmenge", "Vektorraum",
            "Mathematik", "Median", "Kumulante", "Indikatorfunktion", "Zufallsvariable", "Mittelwert",
            "Quantenphysik", "Komplexe_Zahl", "Biologie", "Wirtschaft", "Chemie", "Technik", "Physik");

    /**
     * Erzeugt "ArticleBeingRead"-Events in der angegebenen Frequenz, also als Stream
     *
     * @param interval interval size in time units (see below)
     * @param unit     time units to use for the interval size
     * @return Wiki Aritikel, der gerade gelesen wird
     */
    public Observable<String> wikiArticleBeingReadObservable(final long interval, final TimeUnit unit) {
        final Random randomGenerator = new Random();
        return Observable.interval(interval, unit)
                .map(time -> {
                    String article = WIKI_ARTICLES.get(randomGenerator.nextInt(WIKI_ARTICLES.size()));
                    print("wikiArticleBeingReadObservable=%s", article);
                    return article;
                });
    }

    /**
     * Erzeugt "ArticleBeingRead"-Events, also als Stream.
     * - Es gibt immer wieder Bursts, dann kommen sehr viele Elemente in sehr kurzer Zeit
     *
     * @return Wiki Aritikel, der gerade gelesen wird
     */
    public Observable<String> wikiArticleBeingReadObservableBurst() {
        final Random randomGenerator = new Random();
        return ReactiveUtil.burstSource()
                .map(ignore -> {
                    String article = WIKI_ARTICLES.get(randomGenerator.nextInt(WIKI_ARTICLES.size()));
                    System.out.println(getThreadId() + "wikiArticleBeingReadObservable=" + article);
                    return article;
                });
    }

    /**
     * Erzeugt "ArticleBeingRead"-Events, also als Stream.
     * - Es gibt immer wieder Bursts, dann kommen sehr viele Elemente in sehr kurzer Zeit
     * - Manchmal gibt es einen Fehler (Exception)
     *
     * @return Wiki Aritikel, der gerade gelesen wird
     */
    public Observable<String> wikiArticleBeingReadObservableWithRandomErrors() {
        final Random randomGenerator = new Random();
        return wikiArticleBeingReadObservableBurst().map(article -> {
            if (randomGenerator.nextInt() % 20 == 0) {
                throw new IllegalStateException("something's wrong");
            }
            return article;
        });
    }
}
