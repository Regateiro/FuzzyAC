/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.parameters;

/**
 * Get the list of pages to work on by executing the specified query module.
 * @author DiogoJosé
 */
public enum GeneratorValue {

    /**
     * Enumerate all categories.
     */
    allcategories,
    /**
     * List all deleted revisions by a user or in a namespace.
     */
    alldeletedrevisions,
    /**
     * List all file usages, including non-existing.
     */
    allfileusages,
    /**
     * Enumerate all images sequentially.
     */
    allimages,
    /**
     * Enumerate all links that point to a given namespace.
     */
    alllinks,
    /**
     * Enumerate all pages sequentially in a given namespace.
     */
    allpages,
    /**
     * List all redirects to a namespace.
     */
    allredirects,
    /**
     * List all revisions.
     */
    allrevisions,
    /**
     * List all transclusions (pages embedded using {{x}}), including
     * non-existing.
     */
    alltransclusions,
    /**
     * Find all pages that link to the given page.
     */
    backlinks,
    /**
     * List all categories the pages belong to.
     */
    categories,
    /**
     * List all pages in a given category.
     */
    categorymembers,
    /**
     * Query Content Translation database for translations.
     */
    contenttranslation,
    /**
     * Get suggestion lists for Content Translation.
     */
    contenttranslationsuggestions,
    /**
     * Get deleted revision information.
     */
    deletedrevisions,
    /**
     * List all files that are duplicates of the given files based on hash
     * values.
     */
    duplicatefiles,
    /**
     * Find all pages that embed (transclude) the given title.
     */
    embeddedin,
    /**
     * Enumerate pages that contain a given URL.
     */
    exturlusage,
    /**
     * Find all pages that use the given files.
     */
    fileusage,
    /**
     * Returns pages having coordinates that are located in a certain area.
     */
    geosearch,
    /**
     * This API is for getting a list of one or more pages related to a
     * particular GettingStarted task.
     */
    gettingstartedgetpages,
    /**
     * Returns all files contained on the given pages.
     */
    images,
    /**
     * Find all pages that use the given image title.
     */
    imageusage,
    /**
     * Find all pages that link to the given interwiki link.
     */
    iwbacklinks,
    /**
     * Find all pages that link to the given language link.
     */
    langbacklinks,
    /**
     * Returns all links from the given pages.
     */
    links,
    /**
     * Find all pages that link to the given pages.
     */
    linkshere,
    /**
     * Lists the most viewed pages (based on last day's pageview count).
     */
    mostviewed,
    /**
     * Enumerates pages that have changes pending review.
     */
    oldreviewedpages,
    /**
     * List all pages using a given page property.
     */
    pageswithprop,
    /**
     * Perform a prefix search for page titles.
     */
    prefixsearch,
    /**
     * List all pages associated with one or more projects.
     */
    projectpages,
    /**
     * List all titles protected from creation.
     */
    protectedtitles,
    /**
     * Get a list provided by a QueryPage-based special page.
     */
    querypage,
    /**
     * Get a set of random pages.
     */
    random,
    /**
     * Enumerate recent changes.
     */
    recentchanges,
    /**
     * Returns all redirects to the given pages.
     */
    redirects,
    /**
     * Get revision information.
     */
    revisions,
    /**
     * Perform a full text search.
     */
    search,
    /**
     * Returns all pages transcluded on the given pages.
     */
    templates,
    /**
     * Find all pages that transclude the given pages.
     */
    transcludedin,
    /**
     * Get recent changes to pages in the current user's watchlist.
     */
    watchlist,
    /**
     * Get all pages on the current user's watchlist.
     */
    watchlistraw,
    /**
     * Returns all pages that use the given entity IDs.
     */
    wblistentityusage;
}
