/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/daleharvey/lib/couch-android-launcher/src/org/couchdb/android/ICouchService.aidl
 */
package org.couchdb.android;
public interface ICouchService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.couchdb.android.ICouchService
{
private static final java.lang.String DESCRIPTOR = "org.couchdb.android.ICouchService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.couchdb.android.ICouchService interface,
 * generating a proxy if needed.
 */
public static org.couchdb.android.ICouchService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.couchdb.android.ICouchService))) {
return ((org.couchdb.android.ICouchService)iin);
}
return new org.couchdb.android.ICouchService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_getPort:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getPort();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.couchdb.android.ICouchService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public int getPort() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getPort, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getPort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public int getPort() throws android.os.RemoteException;
}
