/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.parameters;

/**
 * Which properties to get for the queried pages.
 * @author DiogoJosé
 */
public enum PropValue {
    /**
     * List all categories the pages belong to.
     */
    categories,
    /**
     * Returns information about the given categories.
     */
    categoryinfo,
    /**
     * Get the list of logged-in contributors and the count of anonymous
     * contributors to a page.
     */
    contributors,
    /**
     * Returns coordinates of the given pages.
     */
    coordinates,
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
     * Returns all external URLs (not interwikis) from the given pages.
     */
    extlinks,
    /**
     * Returns plain-text or limited HTML extracts of the given pages.
     */
    extracts,
    /**
     * Find all pages that use the given files.
     */
    fileusage,
    /**
     * Get information about the flagging status of the given pages.
     */
    flagged,
    /**
     * Returns global image usage for a certain image.
     */
    globalusage,
    /**
     * Returns file information and upload history.
     */
    imageinfo,
    /**
     * Returns all files contained on the given pages.
     */
    images,
    /**
     * Get basic page information.
     */
    info,
    /**
     * Returns all interwiki links from the given pages.
     */
    iwlinks,
    /**
     * Returns all interlanguage links from the given pages.
     */
    langlinks,
    /**
     * Get the number of other language versions.
     */
    langlinkscount,
    /**
     * Returns all links from the given pages.
     */
    links,
    /**
     * Find all pages that link to the given pages.
     */
    linkshere,
    /**
     * Request all metadata from the page
     */
    mapdata,
    /**
     * Return associated projects and assessments for the given pages.
     */
    pageassessments,
    /**
     * Returns information about images on the page, such as thumbnail and
     * presence of photos.
     */
    pageimages,
    /**
     * Get various page properties defined in the page content.
     */
    pageprops,
    /**
     * Get the Wikidata terms (typically labels, descriptions and aliases)
     * associated with a page via a sitelink. On the entity page itself, the
     * terms are used directly. Caveat: On a repo wiki, this module only works
     * directly on entity pages, not on pages connected to an entity via a
     * sitelink. This may change in the future.
     */
    pageterms,
    /**
     * Shows per-page pageview data (the number of daily pageviews for each of
     * the last pvipdays days).
     */
    pageviews,
    /**
     * Returns all redirects to the given pages.
     */
    redirects,
    /**
     * Return a data representation of references associated with the given
     * pages.
     */
    references,
    /**
     * Get revision information.
     */
    revisions,
    /**
     * Returns file information for stashed files.
     */
    stashimageinfo,
    /**
     * Returns all pages transcluded on the given pages.
     */
    templates,
    /**
     * Find all pages that transclude the given pages.
     */
    transcludedin,
    /**
     * Get transcode status for a given file page.
     */
    transcodestatus,
    /**
     * Extends imageinfo to include video source (derivatives) information
     */
    videoinfo,
    /**
     * Returns all entity IDs used in the given pages.
     */
    wbentityusage;
}
