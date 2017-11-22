/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.parameters;

/**
 * Which lists to get.
 *
 * @author DiogoJosé
 */
public enum ListValue {
    /**
     * Show details of the abuse filters.
     */
    abusefilters,
    /**
     * Show events that were caught by one of the abuse filters.
     */
    abuselog,
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
     * Enumerate all registered users.
     */
    allusers,
    /**
     * Find all pages that link to the given page.
     */
    backlinks,
    /**
     * List all BetaFeatures
     */
    betafeatures,
    /**
     * List all blocked users and IP addresses.
     */
    blocks,
    /**
     * List all pages in a given category.
     */
    categorymembers,
    /**
     * Get a log of campaign configuration changes.
     */
    centralnoticelogs,
    /**
     * Check which IP addresses are used by a given username or which usernames
     * are used by a given IP address.
     */
    checkuser,
    /**
     * Get entries from the CheckUser log.
     */
    checkuserlog,
    /**
     * Query Content Translation database for translations.
     */
    contenttranslation,
    /**
     * Get the section-aligned parallel text for a given translation. See also
     * list=cxpublishedtranslations. Dumps are provided in different formats for
     * high volume access.
     */
    contenttranslationcorpora,
    /**
     * Query Content Translation database for numbers of translations by period
     * of time.
     */
    contenttranslationlangtrend,
    /**
     * Get Content Translation statistics.
     */
    contenttranslationstats,
    /**
     * Get suggestion lists for Content Translation.
     */
    contenttranslationsuggestions,
    /**
     * Fetch all published translations information.
     */
    cxpublishedtranslations,
    /**
     * Fetch the translation statistics for the given user.
     */
    cxtranslatorstats,
    /**
     * Find all pages that embed (transclude) the given title.
     */
    embeddedin,
    /**
     * Enumerate pages that contain a given URL.
     */
    exturlusage,
    /**
     * Enumerate all deleted files sequentially.
     */
    filearchive,
    /**
     * Returns a list of gadget categories.
     */
    gadgetcategories,
    /**
     * Returns a list of gadgets used on this wiki.
     */
    gadgets,
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
     * Enumerate all global users.
     */
    globalallusers,
    /**
     * List all globally blocked IP addresses.
     */
    globalblocks,
    /**
     * Enumerate all global groups.
     */
    globalgroups,
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
     * Get a list of lint errors
     */
    linterrors,
    /**
     * Get events from logs.
     */
    logevents,
    /**
     * Serve autocomplete requests for the site field in MassMessage.
     */
    mmsites,
    /**
     * Lists the most viewed pages (based on last day's pageview count).
     */
    mostviewed,
    /**
     * Get a list of files in the current user's upload stash.
     */
    mystashedfiles,
    /**
     * Enumerates pages that have changes pending review.
     */
    oldreviewedpages,
    /**
     * List all page property names in use on the wiki.
     */
    pagepropnames,
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
     * List all the projects.
     */
    projects,
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
     * Perform a full text search.
     */
    search,
    /**
     * List change tags.
     */
    tags,
    /**
     * Get all edits by a user.
     */
    usercontribs,
    /**
     * Get information about a list of users.
     */
    users,
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
    wblistentityusage,
    /**
     * Enumerate all wiki sets.
     */
    wikisets,
    /**
     * Deprecated. List deleted revisions.
     */
    deletedrevs;
}
