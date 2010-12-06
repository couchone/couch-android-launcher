package org.couchdb.android;

import org.couchdb.android.ICouchClient;

interface ICouchService
{
    void startCouchDB(ICouchClient callback);
    void quitCouchDB(ICouchClient callback);
}
