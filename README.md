Vecna Password Manager
======================

A password manager that uses a GPG encrypted file to safely store your passwords
on your phone.

Setup
-----

First, you will need to create a GPG key pair.  If you don't know what this
means, you are probably not in the target audience for this app.  After you have
created your key pair, export the secret key and copy it to your phone
somewhere.

I'm not sure if this app will work with GPG keys from Android Privacy Guard.  I
intend to do some more testing with that at some point in the future, but the
app meets my requirements so my interest in developing it further wanes.

You will also have to create your password file.  The app cannot edit any
passwords at this time, although I plan on allowing that in the future.  The
format for the password file is rather simple, one entry per line, with three
fields separated by a space.  None of the fields may contain a space.  The
fields are as follows:

    account  - a descriptive name of what the password is for
    username - the username for the account
    password - the password for the account

Once you have created this file, encrypt it and transfer it to your phone.
Personally, I use Dropbox to keep it synchronized, and I plan to build that
functionality into a future version of the app.

Now you should be all set to view your passwords on your phone.

Known Bugs
----------

Currently, if anything goes wrong during decryption, a generic "unknown error"
message is displayed.  It should at least check if the wrong passphrase was
supplied and inform the user of that error.

Planned Features
----------------

 * Synchronization with Dropbox
 * Ability to edit password file
 * Setup walkthrough

License
-------

See the LICENSE file for license details.
