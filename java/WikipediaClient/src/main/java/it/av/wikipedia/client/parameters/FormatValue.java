/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.parameters;

/**
 *
 * @author DiogoJosé
 */
public enum FormatValue {
    /**
     * JSON format
     */
    json,
    /**
     * Pretty print JSON format
     */
    jsonfm,
    /**
     * serialized PHP format
     * @deprecated 
     */
    @Deprecated
    php,
    /**
     * XML format
     * @deprecated
     */
    @Deprecated
    xml,
    /**
     * Pretty print XML format
     * @deprecated
     */
    @Deprecated
    xmlfm;
}
