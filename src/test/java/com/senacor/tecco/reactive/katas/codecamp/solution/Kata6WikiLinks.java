package com.senacor.tecco.reactive.katas.codecamp.solution;

import com.senacor.tecco.reactive.WaitMonitor;
import com.senacor.tecco.reactive.services.WikiService;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.senacor.tecco.reactive.ReactiveUtil.newScheduler;
import static com.senacor.tecco.reactive.ReactiveUtil.print;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * @author Andreas Keefer
 */
public class Kata6WikiLinks {

    private static Scheduler scheduler = newScheduler(25, "io");
    private final WikiService wikiService = new WikiService();

    @Test
    public void linksObservable() throws Exception {
        // 1. lade und parse einen beliebigen Wiki Artikel
        // 2. mappe alle internen Links (parsedPage.getSections().getLinks(Link.type.INTERNAL)) des Artikel auf ein Observable und gib dies auf der Console aus (<Start_Artikel> -> <Link/Artikel_Name>)
        // 3. Wenn das Funktioniert füge eine Rekursion hinzu und lade alle Artikel zu den Links und gib wiederum alle Links auf der Console aus
        // 4. miss die Performance und optimiere die Performance mit Schedulern
        // 5. gib keine Kombination <Start_Artikel> -> <Link/Artikel_Name> mehrfach aus

        final WaitMonitor monitor = new WaitMonitor();

        getLinks("Observable")
                .flatMap(wikiLink -> getLinks(wikiLink.getTargetArticle()))
                .subscribe(
                        next -> {
                        },
                        error -> {
                            System.err.println(error.getMessage());
                            error.printStackTrace();
                        },
                        monitor::complete);

        monitor.waitFor(30, TimeUnit.SECONDS);
    }

    private Observable<WikiLink> getLinks(final String wikiArticle) {
        //print("getLinks fuer Artikel: %s", wikiArticle);
        return wikiService.fetchArticleObservable((wikiArticle))
                .subscribeOn(scheduler)
                .flatMap(wikiService::parseMediaWikiText)
                .subscribeOn(Schedulers.computation())
                .filter(parsedPage -> parsedPage != null)
                .flatMapIterable(ParsedPage::getSections)
                .flatMapIterable(section -> section.getLinks(Link.type.INTERNAL))
                .map(link -> new WikiLink(wikiArticle, link.getTarget()))
                .distinct()
                .doOnNext(wikiLink -> print(wikiLink));
    }

    private static class WikiLink {
        private final String sourceArticle;
        private final String targetArticle;

        public WikiLink(String sourceArticle, String targetArticle) {
            notNull(sourceArticle, "sourceArticle must not be null");
            notNull(targetArticle, "targetArticle must not be null");
            this.sourceArticle = sourceArticle;
            this.targetArticle = targetArticle;
        }

        public String getSourceArticle() {
            return sourceArticle;
        }

        public String getTargetArticle() {
            return targetArticle;
        }

        @Override
        public String toString() {
            return sourceArticle + " -> " + targetArticle;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceArticle, targetArticle);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final WikiLink other = (WikiLink) obj;
            return Objects.equals(this.sourceArticle, other.sourceArticle)
                    && Objects.equals(this.targetArticle, other.targetArticle);
        }
    }
}
