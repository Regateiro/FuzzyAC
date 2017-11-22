/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.parameters;

/**
 * Which metadata to get.
 *
 * @author DiogoJosé
 */
public enum MetaValue {

    /**
     * Return messages from this site.
     */
    allmessages,
    /**
     * Retrieve information about the current authentication status.
     */
    authmanagerinfo,
    /**
     * Get information about what languages the user knows
     */
    babel,
    /**
     * Get a summary of logged API feature usages for a user agent.
     */
    featureusage,
    /**
     * Return meta information about image repositories configured on the wiki.
     */
    filerepoinfo,
    /**
     * Show information about a global user.
     */
    globaluserinfo,
    /**
     * Get number of lint errors in each category
     */
    linterstats,
    /**
     * Get notifications waiting for the current user.
     */
    notifications,
    /**
     * Check to see if two-factor authentication (OATH) is enabled for a user.
     */
    oath,
    /**
     * Return ORES configuration and model data for this wiki.
     */
    ores,
    /**
     * Return general information about the site.
     */
    siteinfo,
    /**
     * Shows sitewide pageview data (daily pageview totals for each of the last
     * pvisdays days).
     */
    siteviews,
    /**
     * Gets tokens for data-modifying actions.
     */
    tokens,
    /**
     * Get pages for which there are unread notifications for the current user.
     */
    unreadnotificationpages,
    /**
     * Get information about the current user.
     */
    userinfo,
    /**
     * Get information about the Wikibase client and the associated Wikibase
     * repository.
     */
    wikibase;
}
