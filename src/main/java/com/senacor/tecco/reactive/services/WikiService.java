package com.senacor.tecco.reactive.services;

import com.senacor.tecco.reactive.ReactiveUtil;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import rx.Observable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.senacor.tecco.reactive.ReactiveUtil.*;

/**
 * @author Andreas Keefer
 */
public class WikiService {

    public static final WikiService WIKI_SERVICE = new WikiService();
    public static final WikiService WIKI_SERVICE_EN = new WikiService("en");

    private final WikipediaServiceJapi WIKIPEDIA_SERVICE_JAPI;
    private static final MediaWikiTextParser MEDIA_WIKI_TEXT_PARSER = new MediaWikiTextParser();

    private final ExecutorService POOL = Executors.newFixedThreadPool(10);

    public WikiService() {
        WIKIPEDIA_SERVICE_JAPI = new WikipediaServiceJapi();
    }

    public WikiService(String language) {
        WIKIPEDIA_SERVICE_JAPI = new WikipediaServiceJapi("http://"+language+".wikipedia.org");
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public Observable<String> fetchArticle(final String wikiArticle) {
        return WIKIPEDIA_SERVICE_JAPI.getArticleObservable(wikiArticle);
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public String fetchArticleString(final String wikiArticle) {
        return WIKIPEDIA_SERVICE_JAPI.getArticle(wikiArticle);
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public void fetchArticleCallback(final String wikiArticle, Consumer<String> articleConsumer, Consumer<Exception> exceptionConsumer) {
        articleConsumer.accept(WIKIPEDIA_SERVICE_JAPI.getArticle(wikiArticle));
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public Future<String> fetchArticleFuture(final String wikiArticle) {
        return POOL.submit(() -> WIKIPEDIA_SERVICE_JAPI.getArticle(wikiArticle));
    }

    /**
     * @param wikiArticle name of the article to be fetched
     * @return fetches a wiki article as a media wiki formated string
     */
    public CompletableFuture<String> fetchArticleCompletableFuture(final String wikiArticle) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> WIKIPEDIA_SERVICE_JAPI.getArticle(wikiArticle));
    }

    //---------------------------------------------------------------------------------

    /**
     * @param text
     * @return
     */
    public String findValue(String text, String key) {
        Pattern pattern = Pattern.compile(key+" ?= ?([\\d,]*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    //---------------------------------------------------------------------------------

    /**
     * @param mediaWikiText Media Wiki formated text
     * @return parsed text in structured form
     */
    public Observable<ParsedPage> parseMediaWikiText(String mediaWikiText) {
        return MEDIA_WIKI_TEXT_PARSER.parseObservable(mediaWikiText);
    }


    /**
     * @param mediaWikiText Media Wiki formated text
     * @return parsed text in structured form
     */
    public ParsedPage parseMediaWikiTextSynchronous(String mediaWikiText) {
        return MEDIA_WIKI_TEXT_PARSER.parse(mediaWikiText);
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

    /**
     * @param parsedPage parsed page
     * @return Sterne Bewertung von 0 bis 5
     */
    public Observable<Integer> rate(final ParsedPage parsedPage) {
        return Observable.create(subscriber -> {
            long start = System.currentTimeMillis();

            if (null == parsedPage) {
                subscriber.onError(new IllegalStateException("parsedPage must not be null"));
                return;
            }
            int articleSize = parsedPage.getText().length();
            int linksCount = parsedPage.getLinks().size();

            fixedDelay(30);

            if (0 == linksCount) {
                // 0 sterne
                subscriber.onNext(0);
                subscriber.onCompleted();
                return;
            }

            final BigDecimal prozent = BigDecimal.valueOf(linksCount)
                    .divide(BigDecimal.valueOf(articleSize), 3, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            final int rating;
            if (prozent.compareTo(BigDecimal.valueOf(0.8)) > 0) {
                // 5 sterne
                rating = 5;
            } else if (prozent.compareTo(BigDecimal.valueOf(0.5)) > 0) {
                // 4 sterne
                rating = 4;
            } else if (prozent.compareTo(BigDecimal.valueOf(0.3)) > 0) {
                // 3 sterne
                rating = 3;
            } else if (prozent.compareTo(BigDecimal.valueOf(0.1)) > 0) {
                // 2 sterne
                rating = 2;
            } else {
                // 1 stern
                rating = 1;
            }

            System.out.println(String.format("%srate: articleSize=%s linksCount=%s prozent=%s runtime=%sms",
                    getThreadId(), articleSize, linksCount, prozent, System.currentTimeMillis() - start));

            subscriber.onNext(rating);
            subscriber.onCompleted();
        });
    }

    /**
     * @param parsedPage parsed page
     * @return Anzahl Woerter im Text
     */
    public Observable<Integer> countWords(final ParsedPage parsedPage) {
        return Observable.create(subscriber -> {
            long start = System.currentTimeMillis();
            if (null == parsedPage) {
                subscriber.onError(new IllegalStateException("parsedPage must not be null"));
                return;
            }
            String text = parsedPage.getText();
            fixedDelay(30);
            int wordCount = new StringTokenizer(text, " ").countTokens();
            System.out.println(String.format("%scountWords: count=%s runtime=%sms",
                    getThreadId(), wordCount, System.currentTimeMillis() - start));
            subscriber.onNext(wordCount);
            subscriber.onCompleted();
        });
    }

    /**
     * @param wikiArticle article
     * @return runtime
     */
    public long save(String wikiArticle) {
        print("save(%s)", wikiArticle);
        int delay = 10;
        fixedDelay(delay);
        return delay;
    }

    /**
     * @param wikiArticle article
     * @return runtime
     */
    public long save(Iterable<String> wikiArticle) {
        print("save(%s)", wikiArticle);
        int delay = 15;
        fixedDelay(delay);
        return delay;
    }
}