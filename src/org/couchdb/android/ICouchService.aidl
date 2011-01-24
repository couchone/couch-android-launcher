package org.couchdb.android;

import org.couchdb.android.ICouchClient;

interface ICouchService
{
    /* Starts couchDB, calls "couchStarted" callback when 
     * complete 
     */
    void initCouchDB(ICouchClient callback);
    
    /* The database may not be named as hinted here, this is to
     * prevent conflicts
     */
    void initDatabase(ICouchClient callback, String name);

    /* This returns the admin credentials of CouchDB, you should
     * really avoid ever using this 
     */
    void adminCredentials(ICouchClient callback);
    
    /*
     * 
     */
    void quitCouchDB();
}
