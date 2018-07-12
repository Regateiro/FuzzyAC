/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public enum RequestType {
    GetPolicy, 
    AddPolicy, 
    GetMetadata, 
    LogInfo,
    LogWarning,
    LogError,
    Decision, 
    AddMetadata, 
    AddSubject, 
    GetSubjectInfo, 
    AddUserContribution, 
    GetUserContributions,
    GetLastUserContribution;
}
