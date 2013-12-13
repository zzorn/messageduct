Messageduct
===========

High level Java networking library with accounts and login handling, as well as Java object serialization.
Uses whitelists to specify what object types are allowed to be transmitted over the network.
Encrypts and compresses network traffic by default.  Plan to implement several systems for password recovery.

In general the library is very configurable if desired, with sane defaults out of the box.
Interfaces are used liberally, and you can easily implement your own version of most sub-services and functionality.


Status
------
Under development, no released versions yet.

TODO:
* Complete full unit testing.
* Account recovery on lost password not implemented yet, might make it a plugable system if it's easy to do.
* Some refactoring, move common utilities to flowutils.
* Write documentation and example.


Licence
-------
LGPL 3.0


Usage
-----

* Instantiate a ClientNetworking instance on the client side, and a ServerNetworking instance on the server side.
* Pass in suitable configuration options in constructors.  Initialize both with calls to init().
* Call clientNetworking.createAccount, pass in desired account name, password, and server address.  Receive a ServerSession.
* Now the client and server can send messages in the form of Java objects to each other.


Contact
------
zzorn@iki.fi


Credits
-------
* Built on top of the Apache Mina network library.
* Uses Kryo for object serialization
* Bouncycastle used for encryption algorithms.
